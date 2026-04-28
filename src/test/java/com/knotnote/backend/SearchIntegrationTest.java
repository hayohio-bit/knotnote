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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SearchIntegrationTest {

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

    private void createNote(String token, String title, String content) throws Exception {
        mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"%s"}
                        """.formatted(title, content)));
    }

    // ── 키워드 검색 ────────────────────────────────────────────────────────

    @Test
    void search_by_title_keyword_returns_matching_notes() throws Exception {
        String token = signupAndLogin("search1@test.com");
        createNote(token, "Spring Boot Guide", "Content about Spring");
        createNote(token, "React Tutorial", "Content about React");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring Boot Guide"));
    }

    @Test
    void search_by_content_keyword_returns_matching_notes() throws Exception {
        String token = signupAndLogin("search2@test.com");
        createNote(token, "Note 1", "Java is great for backend development");
        createNote(token, "Note 2", "Python is used for data science");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "backend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Note 1"));
    }

    @Test
    void search_returns_empty_when_no_match() throws Exception {
        String token = signupAndLogin("search3@test.com");
        createNote(token, "Spring Note", "Spring content");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "nonexistentkeyword12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void search_does_not_return_other_users_notes() throws Exception {
        String tokenA = signupAndLogin("searchA@test.com");
        String tokenB = signupAndLogin("searchB@test.com");
        createNote(tokenA, "Private Spring Note", "Spring content");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + tokenB)
                .param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void search_does_not_return_deleted_notes() throws Exception {
        String token = signupAndLogin("search4@test.com");
        createNote(token, "Delete Me Spring", "content");

        // 메모 ID 조회 후 삭제
        var listResult = mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer " + token))
                .andReturn();
        long noteId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void search_supports_pagination() throws Exception {
        String token = signupAndLogin("search5@test.com");
        for (int i = 1; i <= 5; i++) {
            createNote(token, "Paginated Note " + i, "keyword content");
        }

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "keyword")
                .param("size", "2")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void search_with_blank_q_returns_400() throws Exception {
        String token = signupAndLogin("search6@test.com");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token)
                .param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void search_without_q_returns_4xx() throws Exception {
        String token = signupAndLogin("search7@test.com");

        mockMvc.perform(get("/api/search")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    // ── 시맨틱 검색 ────────────────────────────────────────────────────────

    @Test
    void semantic_search_returns_200_with_empty_list_when_server_disabled() throws Exception {
        // 테스트 환경은 embed.server.enabled=false → 임베딩 없음 → 빈 배열 반환
        String token = signupAndLogin("search8@test.com");
        createNote(token, "Spring 관련 노트", "Spring Boot 내용");

        mockMvc.perform(get("/api/search/semantic")
                .header("Authorization", "Bearer " + token)
                .param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void semantic_search_without_q_returns_4xx() throws Exception {
        String token = signupAndLogin("search9@test.com");

        mockMvc.perform(get("/api/search/semantic")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }
}
