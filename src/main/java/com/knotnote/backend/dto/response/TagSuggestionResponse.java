package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagSuggestionResponse {

    private Long   tagId;
    private String tagName;

    /**
     * 신뢰도 점수 [0.0, 1.0]
     * = 이 태그를 가진 유사 노트들의 (count * avg_similarity) 정규화 값
     */
    private double confidence;
}
