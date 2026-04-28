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
class TagIntegrationTest {

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

    private long createNote(String token, String title) throws Exception {
        var result = mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","content":"content"}
                        """.formatted(title)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    // ── 태그 CRUD ──────────────────────────────────────────────────────────

    @Test
    void create_tag_returns_201() throws Exception {
        String token = signupAndLogin("tag1@test.com");

        mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"backend"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("backend"))
                .andExpect(jsonPath("$.data.noteCount").value(0));
    }

    @Test
    void get_tags_returns_list_with_note_count() throws Exception {
        String token = signupAndLogin("tag2@test.com");
        long tagId = createTag(token, "spring");
        long noteId = createNote(token, "Note");

        // 메모에 태그 연결
        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)));

        mockMvc.perform(get("/api/tags")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("spring"))
                .andExpect(jsonPath("$.data[0].noteCount").value(1));
    }

    @Test
    void create_duplicate_tag_returns_409() throws Exception {
        String token = signupAndLogin("tag3@test.com");
        createTag(token, "java");

        mockMvc.perform(post("/api/tags")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"java"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void delete_tag_returns_200() throws Exception {
        String token = signupAndLogin("tag4@test.com");
        long tagId = createTag(token, "to-delete");

        mockMvc.perform(delete("/api/tags/" + tagId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 삭제 후 목록에서 사라짐 확인
        mockMvc.perform(get("/api/tags")
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ── 메모-태그 연결 ─────────────────────────────────────────────────────

    @Test
    void add_tag_to_note_returns_201_and_tag_appears_in_note() throws Exception {
        String token = signupAndLogin("tag5@test.com");
        long tagId  = createTag(token, "java");
        long noteId = createNote(token, "Java Note");

        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 메모 단건 조회 시 태그 포함 확인
        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.tags[0].name").value("java"));
    }

    @Test
    void add_duplicate_tag_to_note_returns_409() throws Exception {
        String token = signupAndLogin("tag6@test.com");
        long tagId  = createTag(token, "dup");
        long noteId = createNote(token, "Note");

        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)));

        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)))
                .andExpect(status().isConflict());
    }

    @Test
    void remove_tag_from_note_returns_200() throws Exception {
        String token = signupAndLogin("tag7@test.com");
        long tagId  = createTag(token, "temp");
        long noteId = createNote(token, "Note");

        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)));

        mockMvc.perform(delete("/api/notes/" + noteId + "/tags/" + tagId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.tags").isEmpty());
    }
}
