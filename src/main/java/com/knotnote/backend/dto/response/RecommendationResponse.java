package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/notes/{id}/recommendations 응답 DTO
 * 그래프 기반 추천 노트 목록 (점수 내림차순)
 */
@Getter
@Builder
public class RecommendationResponse {

    private Long id;
    private String title;
    private String preview;
    private List<NoteDetailResponse.TagRef> tags;
    private double score;        // 0.0 ~ 1.0 추천 점수
    private String scoreReason;  // "공통 태그 3개 · 공통 이웃 2개"
}
