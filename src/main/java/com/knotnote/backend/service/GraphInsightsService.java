package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.GraphInsightsResponse;

public interface GraphInsightsService {
    /** 사용자의 지식 그래프 구조 분석 결과 반환 */
    GraphInsightsResponse getInsights(Long userId);
}
