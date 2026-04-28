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
 * Phase 3 신규 기능 통합 테스트
 * - Logout (POST /api/auth/logout)
 * - Profile Update (PATCH /api/users/me)
 * - Stats Dashboard (GET /api/stats)
 * - AI Tag Suggestions (GET /api/notes/{noteId}/tag-suggestions)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Phase3IntegrationTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    // ── 공통 헬퍼 ──────────────────────────────────────────────────

    /** 회원가입 후 accessToken + refreshToken 반환 */
    private String[] signupAndGetTokens(String email) throws Exception {
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

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new String[]{
                data.path("accessToken").asText(),
                data.path("refreshToken").asText()
        };
    }

    private String signupAndLogin(String email) throws Exception {
        return signupAndGetTokens(email)[0];
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
        mockMvc.perform(post("/api/notes/" + noteId + "/tags?tagId=" + tagId)
                .header("Authorization", "Bearer " + token));
    }

    private void linkNotes(String token, long fromNoteId, long toNoteId) throws Exception {
        mockMvc.perform(post("/api/notes/" + fromNoteId + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(toNoteId)));
    }

    // ════════════════════════════════════════════════════════════════
    // Logout
    // ════════════════════════════════════════════════════════════════

    @Test
    void logout_with_valid_refresh_token_returns_200() throws Exception {
        String[] tokens = signupAndGetTokens("logout1@test.com");
        String refreshToken = tokens[1];

        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_is_idempotent_for_nonexistent_token() throws Exception {
        // 존재하지 않는 토큰으로도 200 반환 (멱등)
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"nonexistent.token.value"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void logout_then_refresh_returns_401() throws Exception {
        String[] tokens = signupAndGetTokens("logout2@test.com");
        String refreshToken = tokens[1];

        // 로그아웃
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)));

        // 로그아웃 후 같은 refreshToken으로 갱신 시도 → 실패
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_without_body_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ════════════════════════════════════════════════════════════════
    // Profile Update (PATCH /api/users/me)
    // ════════════════════════════════════════════════════════════════

    @Test
    void update_nickname_returns_updated_user() throws Exception {
        String token = signupAndLogin("profile1@test.com");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname":"newNick"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("newNick"));
    }

    @Test
    void update_password_with_correct_current_password_succeeds() throws Exception {
        String token = signupAndLogin("profile2@test.com");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"password1!","newPassword":"newPass2@"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_password_with_wrong_current_password_returns_401() throws Exception {
        String token = signupAndLogin("profile3@test.com");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"wrongPass!","newPassword":"newPass2@"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void update_nickname_too_short_returns_400() throws Exception {
        String token = signupAndLogin("profile4@test.com");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname":"x"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_new_password_too_short_returns_400() throws Exception {
        String token = signupAndLogin("profile5@test.com");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword":"password1!","newPassword":"short"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_profile_without_token_returns_401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nickname":"hacker"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ════════════════════════════════════════════════════════════════
    // Stats Dashboard (GET /api/stats)
    // ════════════════════════════════════════════════════════════════

    @Test
    void stats_returns_zero_for_empty_account() throws Exception {
        String token = signupAndLogin("stats1@test.com");

        mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNotes").value(0))
                .andExpect(jsonPath("$.data.totalLinks").value(0))
                .andExpect(jsonPath("$.data.totalTags").value(0))
                .andExpect(jsonPath("$.data.crystallizationRate").value(0.0))
                .andExpect(jsonPath("$.data.topTags").isArray())
                .andExpect(jsonPath("$.data.vitalityDistribution").exists());
    }

    @Test
    void stats_counts_notes_correctly() throws Exception {
        String token = signupAndLogin("stats2@test.com");
        createNote(token, "Note A", "Content A");
        createNote(token, "Note B", "Content B");
        createNote(token, "Note C", "Content C");

        mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotes").value(3))
                .andExpect(jsonPath("$.data.recentNoteCount").value(3));
    }

    @Test
    void stats_counts_links_and_crystallization_rate() throws Exception {
        String token = signupAndLogin("stats3@test.com");
        long n1 = createNote(token, "N1", "c1");
        long n2 = createNote(token, "N2", "c2");
        long n3 = createNote(token, "N3", "c3");
        linkNotes(token, n1, n2);
        linkNotes(token, n1, n3);

        var result = mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLinks").value(2))
                .andExpect(jsonPath("$.data.crystallizedLinks").value(0))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertEquals(0.0, data.path("crystallizationRate").asDouble(), 0.001);
    }

    @Test
    void stats_shows_top_tags_sorted_by_note_count() throws Exception {
        String token = signupAndLogin("stats4@test.com");
        long n1 = createNote(token, "N1", "c1");
        long n2 = createNote(token, "N2", "c2");
        long t1 = createTag(token, "popular");
        long t2 = createTag(token, "rare");
        addTagToNote(token, n1, t1);
        addTagToNote(token, n2, t1);  // popular: 2 notes
        addTagToNote(token, n1, t2);  // rare: 1 note

        var result = mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topTags").isArray())
                .andReturn();

        JsonNode topTags = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("topTags");
        assertTrue(topTags.size() > 0, "topTags should not be empty");
        assertEquals("popular", topTags.get(0).path("tagName").asText());
        assertEquals(2, topTags.get(0).path("noteCount").asInt());
    }

    @Test
    void stats_vitality_distribution_sums_to_total_notes() throws Exception {
        String token = signupAndLogin("stats5@test.com");
        createNote(token, "N1", "c1");
        createNote(token, "N2", "c2");

        var result = mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        long totalNotes = data.path("totalNotes").asLong();
        JsonNode dist   = data.path("vitalityDistribution");
        long sum = dist.path("high").asLong()
                 + dist.path("medium").asLong()
                 + dist.path("low").asLong();
        assertEquals(totalNotes, sum, "Vitality distribution should sum to totalNotes");
    }

    @Test
    void stats_most_connected_note_has_highest_link_count() throws Exception {
        String token = signupAndLogin("stats6@test.com");
        long hub  = createNote(token, "Hub",  "center");
        long n2   = createNote(token, "N2",   "c2");
        long n3   = createNote(token, "N3",   "c3");
        linkNotes(token, hub, n2);
        linkNotes(token, hub, n3);

        var result = mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode mostConnected = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("mostConnectedNote");
        assertFalse(mostConnected.isMissingNode(), "mostConnectedNote should exist");
        assertEquals("Hub", mostConnected.path("title").asText());
        assertEquals(2, mostConnected.path("linkCount").asInt());
    }

    @Test
    void stats_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void stats_is_isolated_between_users() throws Exception {
        String tokenA = signupAndLogin("statsA@test.com");
        String tokenB = signupAndLogin("statsB@test.com");

        createNote(tokenA, "A's Note", "content");

        // User B 통계에는 A의 노트가 보이지 않아야 함
        mockMvc.perform(get("/api/stats")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalNotes").value(0));
    }

    // ════════════════════════════════════════════════════════════════
    // AI Tag Suggestions (GET /api/notes/{noteId}/tag-suggestions)
    // ════════════════════════════════════════════════════════════════

    @Test
    void tag_suggestions_returns_empty_when_no_other_notes() throws Exception {
        String token = signupAndLogin("tagsug1@test.com");
        long noteId = createNote(token, "Solo Note", "content");

        mockMvc.perform(get("/api/notes/" + noteId + "/tag-suggestions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void tag_suggestions_excludes_already_tagged_tags() throws Exception {
        String token = signupAndLogin("tagsug2@test.com");

        long n1 = createNote(token, "Note 1", "Spring Java");
        long n2 = createNote(token, "Note 2", "Spring Boot");
        long tSpring  = createTag(token, "spring");
        long tBackend = createTag(token, "backend");

        // n1, n2 모두에 spring 태그
        addTagToNote(token, n1, tSpring);
        addTagToNote(token, n2, tSpring);
        addTagToNote(token, n2, tBackend);

        // n1에 spring이 이미 있으므로 추천에서 제외되어야 함
        var result = mockMvc.perform(get("/api/notes/" + n1 + "/tag-suggestions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode suggestions = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        // spring은 이미 붙어있으므로 추천에 없어야 함
        for (JsonNode suggestion : suggestions) {
            assertNotEquals("spring", suggestion.path("tagName").asText(),
                    "Already-tagged 'spring' should not appear in suggestions");
        }
    }

    @Test
    void tag_suggestions_returns_confidence_in_0_to_1_range() throws Exception {
        String token = signupAndLogin("tagsug3@test.com");

        long n1 = createNote(token, "Note 1", "Java backend");
        long n2 = createNote(token, "Note 2", "Java Spring");
        long t1 = createTag(token, "java");
        addTagToNote(token, n2, t1);

        var result = mockMvc.perform(get("/api/notes/" + n1 + "/tag-suggestions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode suggestions = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        for (JsonNode sug : suggestions) {
            double confidence = sug.path("confidence").asDouble();
            assertTrue(confidence >= 0.0 && confidence <= 1.0,
                    "confidence should be in [0, 1], got: " + confidence);
        }
    }

    @Test
    void tag_suggestions_respects_topN_param() throws Exception {
        String token = signupAndLogin("tagsug4@test.com");

        // 노트 여러 개 + 여러 태그 세팅
        long n1 = createNote(token, "Target Note", "content");
        for (int i = 1; i <= 6; i++) {
            long ni = createNote(token, "Note " + i, "content " + i);
            long ti = createTag(token, "tag" + i);
            addTagToNote(token, ni, ti);
        }

        mockMvc.perform(get("/api/notes/" + n1 + "/tag-suggestions?topN=3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(3)));
    }

    @Test
    void tag_suggestions_for_nonexistent_note_returns_404() throws Exception {
        String token = signupAndLogin("tagsug5@test.com");

        mockMvc.perform(get("/api/notes/999999/tag-suggestions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tag_suggestions_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/notes/1/tag-suggestions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tag_suggestions_response_has_required_fields() throws Exception {
        String token = signupAndLogin("tagsug6@test.com");

        long n1 = createNote(token, "Note 1", "ML content");
        long n2 = createNote(token, "Note 2", "ML deep learning");
        long t1 = createTag(token, "ml");
        addTagToNote(token, n2, t1);

        var result = mockMvc.perform(get("/api/notes/" + n1 + "/tag-suggestions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode suggestions = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data");

        for (JsonNode sug : suggestions) {
            assertTrue(sug.has("tagId"),     "tagId field required");
            assertTrue(sug.has("tagName"),   "tagName field required");
            assertTrue(sug.has("confidence"), "confidence field required");
        }
    }
}
