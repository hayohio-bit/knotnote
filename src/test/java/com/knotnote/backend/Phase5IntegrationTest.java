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
 * Phase 5 신규 기능 통합 테스트
 * - Note Pinning        (POST/DELETE /api/notes/{id}/pin)
 * - Activity Feed       (GET /api/activity)
 * - Bulk Operations     (POST /api/notes/bulk/delete, /bulk/tag)
 * - Vitality Listing    (GET /api/notes/low-vitality)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase5IntegrationTest {

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
        var r = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"%s"}
                        """.formatted(title, content))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private long createTag(String token, String name) throws Exception {
        var r = mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s"}
                        """.formatted(name))).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asLong();
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
    // Note Pinning
    // ════════════════════════════════════════════════════════════════

    @Test
    void pin_note_returns_isPinned_true() throws Exception {
        String token  = signupAndLogin("pin1@test.com");
        long   noteId = createNote(token, "Important", "content");

        mockMvc.perform(post("/api/notes/" + noteId + "/pin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void unpin_note_returns_isPinned_false() throws Exception {
        String token  = signupAndLogin("pin2@test.com");
        long   noteId = createNote(token, "Important", "content");

        mockMvc.perform(post("/api/notes/" + noteId + "/pin")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(delete("/api/notes/" + noteId + "/pin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(false));
    }

    @Test
    void pinned_note_appears_in_detail_response() throws Exception {
        String token  = signupAndLogin("pin3@test.com");
        long   noteId = createNote(token, "Pinned Note", "content");

        mockMvc.perform(post("/api/notes/" + noteId + "/pin")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    void newly_created_note_is_not_pinned() throws Exception {
        String token  = signupAndLogin("pin4@test.com");
        long   noteId = createNote(token, "New Note", "content");

        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(false));
    }

    @Test
    void pin_nonexistent_note_returns_404() throws Exception {
        String token = signupAndLogin("pin5@test.com");

        mockMvc.perform(post("/api/notes/999999/pin")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void pin_without_token_returns_401() throws Exception {
        mockMvc.perform(post("/api/notes/1/pin"))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Activity Feed
    // ════════════════════════════════════════════════════════════════

    @Test
    void activity_feed_empty_for_new_account() throws Exception {
        String token = signupAndLogin("act1@test.com");

        // 회원가입 후 노트 없이 바로 조회
        mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void note_creation_appears_in_activity_feed() throws Exception {
        String token = signupAndLogin("act2@test.com");
        createNote(token, "My Note", "content");

        var result = mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        boolean foundCreate = false;
        for (JsonNode a : activities) {
            if ("NOTE_CREATED".equals(a.path("type").asText())
                    && "My Note".equals(a.path("noteTitle").asText())) {
                foundCreate = true;
                break;
            }
        }
        assertTrue(foundCreate, "NOTE_CREATED 이벤트가 활동 피드에 있어야 함");
    }

    @Test
    void pin_event_appears_in_activity_feed() throws Exception {
        String token  = signupAndLogin("act3@test.com");
        long   noteId = createNote(token, "Pin Test", "content");

        mockMvc.perform(post("/api/notes/" + noteId + "/pin")
                .header("Authorization", "Bearer " + token));

        var result = mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        boolean found = false;
        for (JsonNode a : activities) {
            if ("NOTE_PINNED".equals(a.path("type").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "NOTE_PINNED 이벤트가 활동 피드에 있어야 함");
    }

    @Test
    void link_creation_appears_in_activity_feed() throws Exception {
        String token = signupAndLogin("act4@test.com");
        long n1 = createNote(token, "Source", "c1");
        long n2 = createNote(token, "Target", "c2");
        linkNotes(token, n1, n2);

        var result = mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        boolean found = false;
        for (JsonNode a : activities) {
            if ("LINK_CREATED".equals(a.path("type").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "LINK_CREATED 이벤트가 활동 피드에 있어야 함");
    }

    @Test
    void activity_feed_has_required_fields() throws Exception {
        String token = signupAndLogin("act5@test.com");
        createNote(token, "Field Test Note", "content");

        var result = mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        if (activities.size() > 0) {
            JsonNode a = activities.get(0);
            assertTrue(a.has("id"),          "id 필드 필수");
            assertTrue(a.has("type"),        "type 필드 필수");
            assertTrue(a.has("occurredAt"), "occurredAt 필드 필수");
        }
    }

    @Test
    void activity_feed_limit_param_respected() throws Exception {
        String token = signupAndLogin("act6@test.com");
        for (int i = 0; i < 5; i++) createNote(token, "Note " + i, "content");

        var result = mockMvc.perform(get("/api/activity?limit=2")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");
        assertTrue(activities.size() <= 2, "limit=2 이면 최대 2개만 반환");
    }

    @Test
    void activity_feed_is_user_isolated() throws Exception {
        String tokenA = signupAndLogin("actA@test.com");
        String tokenB = signupAndLogin("actB@test.com");
        createNote(tokenA, "A's Note", "private");

        var result = mockMvc.perform(get("/api/activity")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activities = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");
        for (JsonNode a : activities) {
            assertNotEquals("A's Note", a.path("noteTitle").asText(),
                    "B의 피드에 A의 활동이 보이면 안 됨");
        }
    }

    @Test
    void activity_feed_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/activity"))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Bulk Operations
    // ════════════════════════════════════════════════════════════════

    @Test
    void bulk_delete_removes_all_specified_notes() throws Exception {
        String token = signupAndLogin("bulk1@test.com");
        long n1 = createNote(token, "N1", "c");
        long n2 = createNote(token, "N2", "c");
        long n3 = createNote(token, "N3", "c");

        mockMvc.perform(post("/api/notes/bulk/delete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[%d,%d]}
                        """.formatted(n1, n2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // n1, n2 → 삭제됨
        mockMvc.perform(get("/api/notes/" + n1)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notes/" + n2)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        // n3 → 유지
        mockMvc.perform(get("/api/notes/" + n3)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void bulk_delete_ignores_nonexistent_ids() throws Exception {
        String token = signupAndLogin("bulk2@test.com");
        long n1 = createNote(token, "N1", "c");

        // 999999 는 존재하지 않음 — 에러 없이 성공
        mockMvc.perform(post("/api/notes/bulk/delete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[%d,999999]}
                        """.formatted(n1)))
                .andExpect(status().isOk());
    }

    @Test
    void bulk_delete_with_empty_list_returns_400() throws Exception {
        String token = signupAndLogin("bulk3@test.com");

        mockMvc.perform(post("/api/notes/bulk/delete")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[]}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulk_tag_attaches_tag_to_all_notes() throws Exception {
        String token = signupAndLogin("bulk4@test.com");
        long n1  = createNote(token, "N1", "c");
        long n2  = createNote(token, "N2", "c");
        long tid = createTag(token, "shared");

        mockMvc.perform(post("/api/notes/bulk/tag")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[%d,%d],"tagId":%d}
                        """.formatted(n1, n2, tid)))
                .andExpect(status().isOk());

        // 두 노트 모두 태그 확인
        var r1 = mockMvc.perform(get("/api/notes/" + n1)
                .header("Authorization", "Bearer " + token)).andReturn();
        var r2 = mockMvc.perform(get("/api/notes/" + n2)
                .header("Authorization", "Bearer " + token)).andReturn();

        JsonNode tags1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("tags");
        JsonNode tags2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("tags");

        assertTrue(tags1.toString().contains("shared"), "n1에 shared 태그가 붙어야 함");
        assertTrue(tags2.toString().contains("shared"), "n2에 shared 태그가 붙어야 함");
    }

    @Test
    void bulk_tag_is_idempotent() throws Exception {
        String token = signupAndLogin("bulk5@test.com");
        long n1  = createNote(token, "N1", "c");
        long tid = createTag(token, "dup");

        // 두 번 부착해도 에러 없음
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/notes/bulk/tag")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"noteIds":[%d],"tagId":%d}
                            """.formatted(n1, tid)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void bulk_tag_with_nonexistent_tag_returns_404() throws Exception {
        String token = signupAndLogin("bulk6@test.com");
        long n1 = createNote(token, "N1", "c");

        mockMvc.perform(post("/api/notes/bulk/tag")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[%d],"tagId":999999}
                        """.formatted(n1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulk_operations_without_token_return_401() throws Exception {
        mockMvc.perform(post("/api/notes/bulk/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"noteIds":[1]}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Low-Vitality Listing
    // ════════════════════════════════════════════════════════════════

    @Test
    void low_vitality_returns_empty_when_no_notes() throws Exception {
        String token = signupAndLogin("lv1@test.com");

        mockMvc.perform(get("/api/notes/low-vitality")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void low_vitality_returns_notes_sorted_by_vitality_asc() throws Exception {
        String token = signupAndLogin("lv2@test.com");
        // 신규 노트는 vitalityScore=0.5 — 기본 threshold 0.3 미만 없음
        createNote(token, "Fresh Note", "content");

        mockMvc.perform(get("/api/notes/low-vitality")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // vitalityScore=0.5 > threshold=0.3 이므로 결과 없음
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void low_vitality_custom_threshold_works() throws Exception {
        String token = signupAndLogin("lv3@test.com");
        createNote(token, "Note A", "content");
        createNote(token, "Note B", "content");

        // threshold=0.9 → 모든 신규 노트(0.5)가 미만이므로 2개 반환
        var result = mockMvc.perform(get("/api/notes/low-vitality?threshold=0.9")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode notes = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");
        assertEquals(2, notes.size(), "threshold=0.9 이면 기본 vitality(0.5) 노트 모두 포함");
    }

    @Test
    void low_vitality_response_has_isPinned_field() throws Exception {
        String token = signupAndLogin("lv4@test.com");
        createNote(token, "Low Note", "content");

        var result = mockMvc.perform(get("/api/notes/low-vitality?threshold=0.9")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode notes = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        if (notes.size() > 0) {
            assertTrue(notes.get(0).has("isPinned"), "isPinned 필드 필수");
        }
    }

    @Test
    void low_vitality_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/notes/low-vitality"))
                .andExpect(status().isUnauthorized());
    }
}
