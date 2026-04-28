package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.response.NoteDetailResponse;
import com.knotnote.backend.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 없이 접근 가능한 공유 노트 조회 엔드포인트
 * SecurityConfig 에서 /api/share/** 를 permitAll() 처리해야 함
 */
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Tag(name = "Share", description = "노트 공유 공개 API")
public class ShareController {

    private final NoteService noteService;

    @GetMapping("/{shareToken}")
    @Operation(summary = "공유된 노트 조회 (인증 불필요)",
            description = "공유 토큰으로 노트를 읽기 전용으로 조회합니다. 만료된 링크는 410 반환.")
    public ApiResponse<NoteDetailResponse> getSharedNote(@PathVariable String shareToken) {
        return ApiResponse.ok(noteService.getSharedNote(shareToken));
    }
}
