package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.request.TagCreateRequest;
import com.knotnote.backend.dto.response.TagResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tags", description = "태그 API")
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tags")
    @Operation(summary = "태그 목록 조회")
    public ApiResponse<List<TagResponse>> getTags() {
        return ApiResponse.ok(tagService.getTags(SecurityUtil.currentUserId()));
    }

    @PostMapping("/api/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "태그 생성")
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.ok(tagService.createTag(request, SecurityUtil.currentUserId()));
    }

    @DeleteMapping("/api/tags/{tagId}")
    @Operation(summary = "태그 삭제")
    public ApiResponse<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/notes/{noteId}/tags")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "메모에 태그 연결")
    public ApiResponse<Void> addTagToNote(
            @PathVariable Long noteId,
            @RequestParam Long tagId) {
        tagService.addTagToNote(noteId, tagId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/api/notes/{noteId}/tags/{tagId}")
    @Operation(summary = "메모에서 태그 해제")
    public ApiResponse<Void> removeTagFromNote(
            @PathVariable Long noteId,
            @PathVariable Long tagId) {
        tagService.removeTagFromNote(noteId, tagId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }
}
