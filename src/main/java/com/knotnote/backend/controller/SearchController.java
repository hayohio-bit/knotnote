package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.response.NoteSummaryResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search", description = "검색 API")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "키워드 검색 (제목 + 본문)")
    public ApiResponse<Page<NoteSummaryResponse>> search(
            @RequestParam @NotBlank String q,
            @PageableDefault(size = 20, sort = "updatedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(searchService.search(q, SecurityUtil.currentUserId(), pageable));
    }

    @GetMapping("/semantic")
    @Operation(
            summary = "AI 시맨틱 검색 (임베딩 기반)",
            description = "자연어 검색어를 벡터로 변환해 의미적으로 유사한 노트를 반환합니다. "
                    + "임베딩 서버가 비활성화된 경우 빈 배열을 반환합니다."
    )
    public ApiResponse<List<NoteSummaryResponse>> semanticSearch(
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "10") @Positive int topN) {
        return ApiResponse.ok(
                searchService.semanticSearch(q, SecurityUtil.currentUserId(), topN));
    }
}
