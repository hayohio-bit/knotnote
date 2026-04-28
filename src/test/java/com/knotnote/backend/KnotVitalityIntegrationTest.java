package com.knotnote.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 그래프, 추천, Crystallize, PendingLinks, Decay Alerts 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KnotVitalityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── 헬퍼 ───────────────────────────────────────────────────────────────

    private String signupAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"password1!","nickname":"tester"}
                        """.formatted(email)));

        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"password1!"}
                        """.formatted(email)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private long createNote(String token, String title) throws Exception {
        var result = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"content for %s"}
                        """.formatted(title, title)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private long linkNotes(String token, long fromId, long toId) throws Exception {
        mockMvc.perform(post("/api/notes/" + fromId + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(toId)));

        // 링크 ID를 pending links에서 찾아 반환
        var pending = mockMvc.perform(get("/api/notes/" + fromId + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        var arr = objectMapper.readTree(pending.getResponse().getContentAsString())
                .path("data");
        if (arr.isArray() && arr.size() > 0) {
            return arr.get(0).path("linkId").asLong();
        }
        return -1L;
    }

    private long createTag(String token, String name) throws Exception {
        var result = mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s"}
                        """.formatted(name)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void addTagToNote(String token, long noteId, long tagId) throws Exception {
        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)));
    }

    // ── 지식 그래프 ────────────────────────────────────────────────────────

    @Test
    void graph_with_no_notes_returns_empty_nodes_and_edges() throws Exception {
        String token = signupAndLogin("graph1@test.com");

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodes").isArray())
                .andExpect(jsonPath("$.data.edges").isArray())
                .andExpect(jsonPath("$.data.nodes.length()").value(0))
                .andExpect(jsonPath("$.data.edges.length()").value(0));
    }

    @Test
    void graph_returns_nodes_for_all_notes() throws Exception {
        String token = signupAndLogin("graph2@test.com");
        createNote(token, "Note A");
        createNote(token, "Note B");
        createNote(token, "Note C");

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes.length()").value(3))
                .andExpect(jsonPath("$.data.edges.length()").value(0));
    }

    @Test
    void graph_includes_edges_when_notes_are_linked() throws Exception {
        String token = signupAndLogin("graph3@test.com");
        long noteA = createNote(token, "Node A");
        long noteB = createNote(token, "Node B");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)));

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes.length()").value(2))
                .andExpect(jsonPath("$.data.edges.length()").value(1))
                .andExpect(jsonPath("$.data.edges[0].source").value(noteA))
                .andExpect(jsonPath("$.data.edges[0].target").value(noteB))
                .andExpect(jsonPath("$.data.edges[0].crystallized").value(false));
    }

    @Test
    void graph_node_contains_vitality_score_and_degree() throws Exception {
        String token = signupAndLogin("graph4@test.com");
        long noteA = createNote(token, "Hub Note");
        long noteB = createNote(token, "Spoke B");
        long noteC = createNote(token, "Spoke C");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)));
        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteC)));

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes[?(@.id==" + noteA + ")].degree").value(2))
                .andExpect(jsonPath("$.data.nodes[?(@.id==" + noteA + ")].vitalityScore").isNotEmpty());
    }

    @Test
    void graph_does_not_include_deleted_notes() throws Exception {
        String token = signupAndLogin("graph5@test.com");
        long noteA = createNote(token, "Alive Note");
        long noteB = createNote(token, "Delete Me Note");

        mockMvc.perform(delete("/api/notes/" + noteB)
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodes.length()").value(1))
                .andExpect(jsonPath("$.data.nodes[0].id").value(noteA));
    }

    // ── 스마트 연결 추천 ────────────────────────────────────────────────────

    @Test
    void recommendations_returns_empty_when_no_candidates() throws Exception {
        String token = signupAndLogin("rec1@test.com");
        long noteA = createNote(token, "Solo Note");

        mockMvc.perform(get("/api/notes/" + noteA + "/recommendations")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void recommendations_excludes_already_linked_notes() throws Exception {
        String token = signupAndLogin("rec2@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");
        long noteC = createNote(token, "Note C");

        long tagId = createTag(token, "rec-tag");
        addTagToNote(token, noteA, tagId);
        addTagToNote(token, noteB, tagId);
        addTagToNote(token, noteC, tagId);

        // A-B 링크 생성
        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)));

        // A의 추천 결과에 B는 없어야 하고 C는 있어야 함
        var result = mockMvc.perform(get("/api/notes/" + noteA + "/recommendations")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        var data = objectMapper.readTree(body).path("data");

        boolean containsB = false;
        boolean containsC = false;
        for (var node : data) {
            if (node.path("id").asLong() == noteB) containsB = true;
            if (node.path("id").asLong() == noteC) containsC = true;
        }

        assertFalse(containsB, "이미 연결된 noteB가 추천에 포함되면 안 됨");
        assertTrue(containsC,  "공통 태그가 있는 noteC가 추천에 포함돼야 함");
    }

    @Test
    void recommendations_top_n_limits_results() throws Exception {
        String token = signupAndLogin("rec3@test.com");
        long tagId = createTag(token, "common-tag");

        long pivot = createNote(token, "Pivot Note");
        addTagToNote(token, pivot, tagId);

        for (int i = 1; i <= 6; i++) {
            long n = createNote(token, "Candidate " + i);
            addTagToNote(token, n, tagId);
        }

        mockMvc.perform(get("/api/notes/" + pivot + "/recommendations")
                .header("Authorization", "Bearer " + token)
                .param("topN", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ── Crystallize Mode ───────────────────────────────────────────────────

    @Test
    void crystallize_link_returns_updated_strength_and_summary() throws Exception {
        String token = signupAndLogin("cry1@test.com");
        long noteA = createNote(token, "Cry Note A");
        long noteB = createNote(token, "Cry Note B");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d,"intent":"관련 개념 연결"}
                        """.formatted(noteB)));

        // pending links에서 linkId 획득
        var pendingResult = mockMvc.perform(get("/api/notes/" + noteA + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        long linkId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
                .path("data").get(0).path("linkId").asLong();

        // Crystallize
        mockMvc.perform(patch("/api/notes/" + noteA + "/links/" + linkId + "/crystallize")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"summary":"두 메모는 인증 흐름의 앞뒤 단계를 설명한다."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.linkId").value(linkId))
                .andExpect(jsonPath("$.data.newStrength").isNumber())
                .andExpect(jsonPath("$.data.crystallizeSummary").value("두 메모는 인증 흐름의 앞뒤 단계를 설명한다."))
                .andExpect(jsonPath("$.data.crystallizedAt").isNotEmpty());
    }

    @Test
    void crystallized_link_appears_as_crystallized_in_graph() throws Exception {
        String token = signupAndLogin("cry2@test.com");
        long noteA = createNote(token, "Graph Cry A");
        long noteB = createNote(token, "Graph Cry B");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)));

        var pendingResult = mockMvc.perform(get("/api/notes/" + noteA + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        long linkId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
                .path("data").get(0).path("linkId").asLong();

        mockMvc.perform(patch("/api/notes/" + noteA + "/links/" + linkId + "/crystallize")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"summary":"연결 확정"}
                        """));

        mockMvc.perform(get("/api/notes/graph")
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.edges[0].crystallized").value(true))
                .andExpect(jsonPath("$.data.edges[0].crystallizeSummary").value("연결 확정"));
    }

    @Test
    void crystallize_with_wrong_link_id_returns_404() throws Exception {
        String token = signupAndLogin("cry3@test.com");
        long noteA = createNote(token, "Note A");

        mockMvc.perform(patch("/api/notes/" + noteA + "/links/99999/crystallize")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"summary":"should fail"}
                        """))
                .andExpect(status().isNotFound());
    }

    // ── Pending Links ──────────────────────────────────────────────────────

    @Test
    void pending_links_returns_uncrystallized_links_only() throws Exception {
        String token = signupAndLogin("pend1@test.com");
        long noteA = createNote(token, "Pend Note A");
        long noteB = createNote(token, "Pend Note B");
        long noteC = createNote(token, "Pend Note C");

        // A-B 링크 생성 후 crystallize
        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)));

        var pendingResult1 = mockMvc.perform(get("/api/notes/" + noteA + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andReturn();
        long linkId = objectMapper.readTree(pendingResult1.getResponse().getContentAsString())
                .path("data").get(0).path("linkId").asLong();

        mockMvc.perform(patch("/api/notes/" + noteA + "/links/" + linkId + "/crystallize")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"summary":"확정"}
                        """));

        // A-C 링크는 미확정 상태로 유지
        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteC)));

        // pending links는 A-C 링크 1개만 반환해야 함
        mockMvc.perform(get("/api/notes/" + noteA + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].toNoteId").value(noteC));
    }

    @Test
    void pending_links_empty_when_no_links() throws Exception {
        String token = signupAndLogin("pend2@test.com");
        long noteA = createNote(token, "Isolated");

        mockMvc.perform(get("/api/notes/" + noteA + "/links/pending")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── Knot Decay Alerts ──────────────────────────────────────────────────

    @Test
    void decay_alerts_returns_ok_with_empty_or_list() throws Exception {
        String token = signupAndLogin("decay1@test.com");

        // 노트가 없어도 200 + 빈 배열 반환
        mockMvc.perform(get("/api/notes/decay-alerts")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void decay_alerts_response_has_expected_fields_when_note_exists() throws Exception {
        String token = signupAndLogin("decay2@test.com");

        // 허브 조건(degree >= 2)을 충족하는 노트 구성
        long hub = createNote(token, "Hub Note");
        long spokeA = createNote(token, "Spoke A");
        long spokeB = createNote(token, "Spoke B");

        mockMvc.perform(post("/api/notes/" + hub + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(spokeA)));
        mockMvc.perform(post("/api/notes/" + hub + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(spokeB)));

        // 응답 구조 확인 (필드 존재 여부)
        var result = mockMvc.perform(get("/api/notes/decay-alerts")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        // 항목이 있을 경우 필수 필드 검증
        var data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        if (data.size() > 0) {
            var first = data.get(0);
            assertTrue(first.has("noteId"),             "noteId 필드 필요");
            assertTrue(first.has("noteTitle"),          "noteTitle 필드 필요");
            assertTrue(first.has("vitalityScore"),      "vitalityScore 필드 필요");
            assertTrue(first.has("connectedNoteCount"), "connectedNoteCount 필드 필요");
        }
    }
}
