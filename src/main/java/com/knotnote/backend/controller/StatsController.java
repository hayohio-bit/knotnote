package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.response.GraphInsightsResponse;
import com.knotnote.backend.dto.response.StatsResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.GraphInsightsService;
import com.knotnote.backend.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Stats", description = "지식 그래프 통계 API")
public class StatsController {

    private final StatsService         statsService;
    private final GraphInsightsService graphInsightsService;

    @GetMapping
    @Operation(
            summary = "대시보드 통계 조회",
            description = "노트 수, 링크 수, Crystallize 완성률, 태그 분포, "
                    + "Vitality 분포, 평균 활성도, 최다 연결 노트를 반환합니다."
    )
    public ApiResponse<StatsResponse> getStats() {
        return ApiResponse.ok(statsService.getStats(SecurityUtil.currentUserId()));
    }

    @GetMapping("/graph-insights")
    @Operation(
            summary = "그래프 구조 분석",
            description = "고아 노트, 허브 노트 Top5, 약한 연결 Top10, "
                    + "클러스터 수, 연결률, Crystallize 완성률을 반환합니다."
    )
    public ApiResponse<GraphInsightsResponse> getGraphInsights() {
        return ApiResponse.ok(graphInsightsService.getInsights(SecurityUtil.currentUserId()));
    }
}
