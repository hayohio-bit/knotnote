package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.StatsResponse;

public interface StatsService {
    /** 로그인한 사용자의 지식 그래프 통계 */
    StatsResponse getStats(Long userId);
}
