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
class NoteIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── 공통 헬퍼 ──────────────────────────────────────────────────────────

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

    // ── 생성 ───────────────────────────────────────────────────────────────

    @Test
    void create_note_returns_201_with_data() throws Exception {
        String token = signupAndLogin("note1@test.com");

        mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"My Note","content":"Hello World"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("My Note"))
                .andExpect(jsonPath("$.data.content").value("Hello World"))
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.linkedNotes").isArray());
    }

    @Test
    void create_note_without_title_returns_400() throws Exception {
        String token = signupAndLogin("note_valid@test.com");

        mockMvc.perform(post("/api/notes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"content":"No title note"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── 목록 / 단건 ────────────────────────────────────────────────────────

    @Test
    void get_notes_returns_paginated_list() throws Exception {
        String token = signupAndLogin("note2@test.com");
        createNote(token, "Note 1", "Content 1");
        createNote(token, "Note 2", "Content 2");

        mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void get_note_by_id_returns_detail() throws Exception {
        String token = signupAndLogin("note3@test.com");
        long noteId = createNote(token, "Detail Note", "Detail Content");

        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(noteId))
                .andExpect(jsonPath("$.data.title").value("Detail Note"))
                .andExpect(jsonPath("$.data.content").value("Detail Content"));
    }

    // ── 수정 ───────────────────────────────────────────────────────────────

    @Test
    void update_note_changes_title_and_content() throws Exception {
        String token = signupAndLogin("note4@test.com");
        long noteId = createNote(token, "Old Title", "Old Content");

        mockMvc.perform(patch("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"New Title","content":"New Content"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("New Title"))
                .andExpect(jsonPath("$.data.content").value("New Content"));
    }

    // ── 삭제 (소프트) ──────────────────────────────────────────────────────

    @Test
    void delete_note_then_get_returns_404() throws Exception {
        String token = signupAndLogin("note5@test.com");
        long noteId = createNote(token, "To Delete", "bye");

        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 소프트 삭제 후 조회 → 404
        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleted_note_not_included_in_list() throws Exception {
        String token = signupAndLogin("note_del_list@test.com");
        long noteId = createNote(token, "Delete Me", "content");
        createNote(token, "Keep Me", "content");

        mockMvc.perform(delete("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get("/api/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── 권한 ───────────────────────────────────────────────────────────────

    @Test
    void access_other_users_note_returns_404() throws Exception {
        String tokenA = signupAndLogin("noteA@test.com");
        String tokenB = signupAndLogin("noteB@test.com");
        long noteId = createNote(tokenA, "Private", "secret");

        mockMvc.perform(get("/api/notes/" + noteId)
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticated_request_returns_4xx() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().is4xxClientError());
    }
}
