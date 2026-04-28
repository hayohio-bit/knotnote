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
class SmartFolderIntegrationTest {

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
        mockMvc.perform(post("/api/notes/" + noteId + "/tags")
                .header("Authorization", "Bearer " + token)
                .param("tagId", String.valueOf(tagId)));
    }

    private long createFolder(String token, String json) throws Exception {
        var result = mockMvc.perform(post("/api/smart-folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    // ── 스마트 폴더 CRUD ──────────────────────────────────────────────────

    @Test
    void create_folder_returns_201_with_data() throws Exception {
        String token = signupAndLogin("sf1@test.com");

        mockMvc.perform(post("/api/smart-folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"My Folder","tagMatchMode":"ANY"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("My Folder"));
    }

    @Test
    void create_folder_without_name_returns_400() throws Exception {
        String token = signupAndLogin("sf_invalid@test.com");

        mockMvc.perform(post("/api/smart-folders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tagMatchMode":"ANY"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void list_folders_returns_created_folders() throws Exception {
        String token = signupAndLogin("sf2@test.com");
        createFolder(token, """
                {"name":"Folder A","tagMatchMode":"ANY"}
                """);
        createFolder(token, """
                {"name":"Folder B","tagMatchMode":"ALL"}
                """);

        mockMvc.perform(get("/api/smart-folders")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void update_folder_changes_name() throws Exception {
        String token = signupAndLogin("sf3@test.com");
        long folderId = createFolder(token, """
                {"name":"Old Name","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(patch("/api/smart-folders/" + folderId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"New Name","tagMatchMode":"ANY"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"));
    }

    @Test
    void delete_folder_removes_it_from_list() throws Exception {
        String token = signupAndLogin("sf4@test.com");
        long folderId = createFolder(token, """
                {"name":"To Delete","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(delete("/api/smart-folders/" + folderId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/smart-folders")
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void access_other_users_folder_returns_404() throws Exception {
        String tokenA = signupAndLogin("sfA@test.com");
        String tokenB = signupAndLogin("sfB@test.com");
        long folderId = createFolder(tokenA, """
                {"name":"Private Folder","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // ── 키워드 필터 ────────────────────────────────────────────────────────

    @Test
    void folder_with_keyword_filter_returns_matching_notes() throws Exception {
        String token = signupAndLogin("sf5@test.com");
        createNote(token, "Spring Boot Guide", "backend framework");
        createNote(token, "React Tutorial", "frontend framework");

        long folderId = createFolder(token, """
                {"name":"Spring Folder","keyword":"Spring","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Spring Boot Guide"));
    }

    @Test
    void folder_with_keyword_filter_matches_content() throws Exception {
        String token = signupAndLogin("sf6@test.com");
        createNote(token, "Note 1", "contains the keyword spring");
        createNote(token, "Note 2", "does not match anything");

        long folderId = createFolder(token, """
                {"name":"Keyword Folder","keyword":"spring","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Note 1"));
    }

    // ── 태그 필터 (ANY) ─────────────────────────────────────────────────────

    @Test
    void folder_with_any_tag_filter_returns_notes_with_at_least_one_tag() throws Exception {
        String token = signupAndLogin("sf7@test.com");
        long tagJava = createTag(token, "java");
        long tagReact = createTag(token, "react");

        long noteA = createNote(token, "Java Note", "about java");
        long noteB = createNote(token, "React Note", "about react");
        long noteC = createNote(token, "Other Note", "no matching tag");

        addTagToNote(token, noteA, tagJava);
        addTagToNote(token, noteB, tagReact);

        long folderId = createFolder(token, """
                {"name":"Java or React","tagIds":[%d,%d],"tagMatchMode":"ANY"}
                """.formatted(tagJava, tagReact));

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ── 태그 필터 (ALL) ─────────────────────────────────────────────────────

    @Test
    void folder_with_all_tag_filter_returns_notes_with_all_tags() throws Exception {
        String token = signupAndLogin("sf8@test.com");
        long tagJava = createTag(token, "java-all");
        long tagSpring = createTag(token, "spring-all");

        long noteA = createNote(token, "Java + Spring", "both tags");
        long noteB = createNote(token, "Java Only", "one tag only");

        addTagToNote(token, noteA, tagJava);
        addTagToNote(token, noteA, tagSpring);
        addTagToNote(token, noteB, tagJava);

        long folderId = createFolder(token, """
                {"name":"Java AND Spring","tagIds":[%d,%d],"tagMatchMode":"ALL"}
                """.formatted(tagJava, tagSpring));

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Java + Spring"));
    }

    // ── 날짜 필터 ──────────────────────────────────────────────────────────

    @Test
    void folder_with_no_filters_returns_all_user_notes() throws Exception {
        String token = signupAndLogin("sf9@test.com");
        createNote(token, "Note 1", "content");
        createNote(token, "Note 2", "content");
        createNote(token, "Note 3", "content");

        long folderId = createFolder(token, """
                {"name":"All Notes","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void folder_notes_do_not_include_other_users_notes() throws Exception {
        String tokenA = signupAndLogin("sf10A@test.com");
        String tokenB = signupAndLogin("sf10B@test.com");

        createNote(tokenA, "User A Note", "content");
        createNote(tokenB, "User B Note", "content");

        long folderId = createFolder(tokenA, """
                {"name":"My Folder","tagMatchMode":"ANY"}
                """);

        mockMvc.perform(get("/api/smart-folders/" + folderId + "/notes")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("User A Note"));
    }
}
