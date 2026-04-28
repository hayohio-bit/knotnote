package com.knotnote.backend;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Phase 4 신규 기능 통합 테스트
 * - Note Version History  (GET /api/notes/{id}/versions, POST /{id}/versions/{vId}/restore)
 * - Graph Insights API    (GET /api/stats/graph-insights)
 * - Knowledge Base Export (GET /api/export?format=json|markdown)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase4IntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── 헬퍼 ───────────────────────────────────────────────────────

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

    private long createNote(String token, String title, String content) throws Exception {
        var result = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"%s"}
                        """.formatted(title, content)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void updateNote(String token, long noteId, String title, String content) throws Exception {
        mockMvc.perform(patch("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"%s"}
                        """.formatted(title, content)));
    }

    private void linkNotes(String token, long from, long to) throws Exception {
        mockMvc.perform(post("/api/notes/" + from + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(to)));
    }

    // ════════════════════════════════════════════════════════════════
    // Note Version History
    // ════════════════════════════════════════════════════════════════

    @Test
    void versions_empty_for_newly_created_note() throws Exception {
        String token  = signupAndLogin("ver1@test.com");
        long   noteId = createNote(token, "Fresh Note", "initial content");

        mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void versions_created_on_each_update() throws Exception {
        String token  = signupAndLogin("ver2@test.com");
        long   noteId = createNote(token, "Title V1", "Content V1");

        updateNote(token, noteId, "Title V2", "Content V2");
        updateNote(token, noteId, "Title V3", "Content V3");

        var result = mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode versions = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        assertEquals(2, versions.size(), "2번 수정 → 버전 2개 생성");
        // 최신순 정렬: versionNumber 2가 먼저
        assertEquals(2, versions.get(0).path("versionNumber").asInt());
        assertEquals("Title V2", versions.get(0).path("title").asText());
    }

    @Test
    void version_has_required_fields() throws Exception {
        String token  = signupAndLogin("ver3@test.com");
        long   noteId = createNote(token, "Note", "Content");
        updateNote(token, noteId, "Note Updated", "Content Updated");

        var result = mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode v = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").get(0);

        assertTrue(v.has("versionId"),     "versionId 필드 필수");
        assertTrue(v.has("versionNumber"), "versionNumber 필드 필수");
        assertTrue(v.has("title"),         "title 필드 필수");
        assertTrue(v.has("content"),       "content 필드 필수");
        assertTrue(v.has("savedAt"),       "savedAt 필드 필수");
    }

    @Test
    void restore_version_reverts_note_content() throws Exception {
        String token  = signupAndLogin("ver4@test.com");
        long   noteId = createNote(token, "Original Title", "Original Content");

        updateNote(token, noteId, "Changed Title", "Changed Content");

        // 버전 목록에서 첫 번째 버전(= original) ID 가져오기
        var listResult = mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        JsonNode versions = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data");
        long versionId = versions.get(0).path("versionId").asLong();

        // 복원
        var restoreResult = mockMvc.perform(
                post("/api/notes/" + noteId + "/versions/" + versionId + "/restore")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode restored = objectMapper.readTree(restoreResult.getResponse().getContentAsString())
                .path("data");

        assertEquals("Original Title",   restored.path("title").asText());
        assertEquals("Original Content", restored.path("content").asText());
    }

    @Test
    void restore_saves_current_state_as_new_version() throws Exception {
        String token  = signupAndLogin("ver5@test.com");
        long   noteId = createNote(token, "V1", "c1");
        updateNote(token, noteId, "V2", "c2");

        // 버전 1개 확인
        var beforeRestore = mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token)).andReturn();
        int countBefore = objectMapper.readTree(beforeRestore.getResponse().getContentAsString())
                .path("data").size();

        long versionId = objectMapper.readTree(beforeRestore.getResponse().getContentAsString())
                .path("data").get(0).path("versionId").asLong();

        // 복원 후 버전 수 증가 확인
        mockMvc.perform(post("/api/notes/" + noteId + "/versions/" + versionId + "/restore")
                .header("Authorization", "Bearer " + token));

        var afterRestore = mockMvc.perform(get("/api/notes/" + noteId + "/versions")
                .header("Authorization", "Bearer " + token)).andReturn();
        int countAfter = objectMapper.readTree(afterRestore.getResponse().getContentAsString())
                .path("data").size();

        assertTrue(countAfter > countBefore, "복원 시 현재 상태도 버전으로 저장되어야 함");
    }

    @Test
    void versions_for_nonexistent_note_returns_404() throws Exception {
        String token = signupAndLogin("ver6@test.com");
        mockMvc.perform(get("/api/notes/999999/versions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void versions_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/notes/1/versions"))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Graph Insights API
    // ════════════════════════════════════════════════════════════════

    @Test
    void graph_insights_empty_for_new_account() throws Exception {
        String token = signupAndLogin("gi1@test.com");

        mockMvc.perform(get("/api/stats/graph-insights")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orphanNotes").isArray())
                .andExpect(jsonPath("$.data.hubNotes").isArray())
                .andExpect(jsonPath("$.data.weakLinks").isArray())
                .andExpect(jsonPath("$.data.clusterCount").value(0))
                .andExpect(jsonPath("$.data.connectivityRate").value(0.0));
    }

    @Test
    void orphan_notes_detected_correctly() throws Exception {
        String token = signupAndLogin("gi2@test.com");
        long n1 = createNote(token, "Orphan", "no links");
        long n2 = createNote(token, "Hub",    "has links");
        long n3 = createNote(token, "Leaf",   "one link");
        linkNotes(token, n2, n3);

        var result = mockMvc.perform(get("/api/stats/graph-insights")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode orphans = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("orphanNotes");

        assertEquals(1, orphans.size(), "고아 노트는 Orphan 1개여야 함");
        assertEquals("Orphan", orphans.get(0).path("title").asText());
    }

    @Test
    void hub_nodes_sorted_by_degree() throws Exception {
        String token = signupAndLogin("gi3@test.com");
        long hub  = createNote(token, "Hub",  "center");
        long n2   = createNote(token, "N2",   "leaf");
        long n3   = createNote(token, "N3",   "leaf");
        long n4   = createNote(token, "N4",   "leaf");
        linkNotes(token, hub, n2);
        linkNotes(token, hub, n3);
        linkNotes(token, hub, n4);

        var result = mockMvc.perform(get("/api/stats/graph-insights")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode hubs = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("hubNotes");

        assertTrue(hubs.size() > 0);
        assertEquals("Hub", hubs.get(0).path("title").asText(),
                "가장 연결이 많은 노트가 첫 번째여야 함");
        assertEquals(3, hubs.get(0).path("degree").asInt());
    }

    @Test
    void cluster_count_equals_connected_components() throws Exception {
        String token = signupAndLogin("gi4@test.com");
        // 3개의 독립 섬: (A-B), (C-D), (E)
        long a = createNote(token, "A", "c"); long b = createNote(token, "B", "c");
        long c = createNote(token, "C", "c"); long d = createNote(token, "D", "c");
        long e = createNote(token, "E", "c");
        linkNotes(token, a, b);
        linkNotes(token, c, d);

        var result = mockMvc.perform(get("/api/stats/graph-insights")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        int clusterCount = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("clusterCount").asInt();

        assertEquals(3, clusterCount, "연결된 컴포넌트 3개 (A-B), (C-D), (E)");
    }

    @Test
    void connectivity_rate_correct() throws Exception {
        String token = signupAndLogin("gi5@test.com");
        long n1 = createNote(token, "N1", "c");
        long n2 = createNote(token, "N2", "c");
        long n3 = createNote(token, "N3", "c"); // 고아
        linkNotes(token, n1, n2);

        var result = mockMvc.perform(get("/api/stats/graph-insights")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        double rate = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("connectivityRate").asDouble();

        // 3개 중 2개 연결 → 2/3 ≈ 0.667
        assertTrue(rate > 0.6 && rate < 0.7,
                "connectivityRate should be ~0.667, got: " + rate);
    }

    @Test
    void graph_insights_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/stats/graph-insights"))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Knowledge Base Export
    // ════════════════════════════════════════════════════════════════

    @Test
    void export_json_returns_zip_file() throws Exception {
        String token = signupAndLogin("exp1@test.com");
        createNote(token, "My Note", "Some content");

        var result = mockMvc.perform(get("/api/export?format=json")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertTrue(contentType != null && contentType.contains("zip"),
                "Content-Type should be application/zip");

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertTrue(disposition != null && disposition.contains("knotnote-export-json.zip"),
                "Filename should be knotnote-export-json.zip");

        byte[] body = result.getResponse().getContentAsByteArray();
        assertTrue(body.length > 0, "ZIP body should not be empty");
    }

    @Test
    void export_markdown_returns_zip_file() throws Exception {
        String token = signupAndLogin("exp2@test.com");
        createNote(token, "Markdown Note", "# Hello");

        var result = mockMvc.perform(get("/api/export?format=markdown")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertTrue(contentType != null && contentType.contains("zip"));

        byte[] body = result.getResponse().getContentAsByteArray();
        assertTrue(body.length > 0);
    }

    @Test
    void export_default_format_is_json() throws Exception {
        String token = signupAndLogin("exp3@test.com");
        createNote(token, "Note", "content");

        var result = mockMvc.perform(get("/api/export")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertTrue(disposition != null && disposition.contains("json"),
                "기본 포맷은 json이어야 함");
    }

    @Test
    void export_invalid_format_returns_400() throws Exception {
        String token = signupAndLogin("exp4@test.com");

        mockMvc.perform(get("/api/export?format=xml")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void export_empty_account_still_returns_valid_zip() throws Exception {
        String token = signupAndLogin("exp5@test.com");

        var result = mockMvc.perform(get("/api/export?format=json")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // ZIP 매직 바이트 확인 (PK\x03\x04)
        byte[] body = result.getResponse().getContentAsByteArray();
        assertTrue(body.length >= 4, "최소 4바이트 이상");
        assertEquals(0x50, body[0] & 0xFF, "ZIP magic byte 0: 'P'");
        assertEquals(0x4B, body[1] & 0xFF, "ZIP magic byte 1: 'K'");
    }

    @Test
    void export_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void export_is_user_isolated() throws Exception {
        String tokenA = signupAndLogin("expA@test.com");
        String tokenB = signupAndLogin("expB@test.com");

        createNote(tokenA, "A's Secret", "private content");

        // B의 export에는 A의 노트가 없어야 함 — zip 내용 직접 파싱
        var result = mockMvc.perform(get("/api/export?format=json")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        byte[] zipBytes = result.getResponse().getContentAsByteArray();
        // ZipInputStream으로 파싱하여 JSON 확인
        try (var zis = new java.util.zip.ZipInputStream(
                new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    String json = new String(zis.readAllBytes());
                    assertFalse(json.contains("A's Secret"),
                            "B의 export에 A의 노트가 포함되면 안 됨");
                }
            }
        }
    }
}
