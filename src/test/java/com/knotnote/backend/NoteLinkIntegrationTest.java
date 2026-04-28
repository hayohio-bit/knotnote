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
class NoteLinkIntegrationTest {

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
                        {"title":"%s","content":"content"}
                        """.formatted(title)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void linkNotes(String token, long noteId, long targetId) throws Exception {
        mockMvc.perform(post("/api/notes/" + noteId + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(targetId)));
    }

    // ── 링크 생성 ──────────────────────────────────────────────────────────

    @Test
    void link_notes_returns_201() throws Exception {
        String token = signupAndLogin("link1@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void link_appears_in_source_note_detail() throws Exception {
        String token = signupAndLogin("link2@test.com");
        long noteA = createNote(token, "Source Note");
        long noteB = createNote(token, "Target Note");

        linkNotes(token, noteA, noteB);

        mockMvc.perform(get("/api/notes/" + noteA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedNotes[0].id").value(noteB));
    }

    @Test
    void link_is_bidirectional() throws Exception {
        String token = signupAndLogin("link3@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");

        linkNotes(token, noteA, noteB);

        // A→B 링크 생성 후 B에서도 A가 보여야 함
        mockMvc.perform(get("/api/notes/" + noteB)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linkedNotes[0].id").value(noteA));
    }

    // ── 링크 목록 조회 ─────────────────────────────────────────────────────

    @Test
    void get_linked_notes_returns_list() throws Exception {
        String token = signupAndLogin("link4@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");
        long noteC = createNote(token, "Note C");

        linkNotes(token, noteA, noteB);
        linkNotes(token, noteA, noteC);

        mockMvc.perform(get("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void get_linked_notes_when_empty_returns_empty_list() throws Exception {
        String token = signupAndLogin("link5@test.com");
        long noteA = createNote(token, "Isolated Note");

        mockMvc.perform(get("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── 링크 해제 ──────────────────────────────────────────────────────────

    @Test
    void unlink_notes_returns_200_and_link_disappears() throws Exception {
        String token = signupAndLogin("link6@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");

        linkNotes(token, noteA, noteB);

        mockMvc.perform(delete("/api/notes/" + noteA + "/links/" + noteB)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── 제약 조건 ──────────────────────────────────────────────────────────

    @Test
    void link_note_to_itself_returns_4xx() throws Exception {
        String token = signupAndLogin("link7@test.com");
        long noteA = createNote(token, "Self Note");

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteA)))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicate_link_returns_409() throws Exception {
        String token = signupAndLogin("link8@test.com");
        long noteA = createNote(token, "Note A");
        long noteB = createNote(token, "Note B");

        linkNotes(token, noteA, noteB);

        mockMvc.perform(post("/api/notes/" + noteA + "/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"targetNoteId":%d}
                        """.formatted(noteB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }
}
