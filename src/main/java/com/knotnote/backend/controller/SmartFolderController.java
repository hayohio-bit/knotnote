package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.request.SmartFolderRequest;
import com.knotnote.backend.dto.response.NoteSummaryResponse;
import com.knotnote.backend.dto.response.SmartFolderResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.SmartFolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-folders")
@RequiredArgsConstructor
@Tag(name = "SmartFolders", description = "스마트 폴더 API")
public class SmartFolderController {

    private final SmartFolderService smartFolderService;

    @GetMapping
    @Operation(summary = "스마트 폴더 목록")
    public ApiResponse<List<SmartFolderResponse>> list() {
        return ApiResponse.ok(smartFolderService.list(SecurityUtil.currentUserId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "스마트 폴더 생성")
    public ApiResponse<SmartFolderResponse> create(@Valid @RequestBody SmartFolderRequest req) {
        return ApiResponse.ok(smartFolderService.create(req, SecurityUtil.currentUserId()));
    }

    @PatchMapping("/{folderId}")
    @Operation(summary = "스마트 폴더 수정")
    public ApiResponse<SmartFolderResponse> update(
            @PathVariable Long folderId,
            @Valid @RequestBody SmartFolderRequest req) {
        return ApiResponse.ok(smartFolderService.update(folderId, req, SecurityUtil.currentUserId()));
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "스마트 폴더 삭제")
    public ApiResponse<Void> delete(@PathVariable Long folderId) {
        smartFolderService.delete(folderId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{folderId}/notes")
    @Operation(summary = "스마트 폴더 노트 목록",
            description = "저장된 필터 조건(태그 ANY/ALL · 키워드 · 기간)에 맞는 노트를 실시간 집계")
    public ApiResponse<List<NoteSummaryResponse>> getNotes(@PathVariable Long folderId) {
        return ApiResponse.ok(smartFolderService.getNotes(folderId, SecurityUtil.currentUserId()));
    }
}
