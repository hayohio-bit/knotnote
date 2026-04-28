package com.knotnote.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NoteDetailResponse {
    private Long id;
    private String title;
    private String content;
    private List<TagRef> tags;
    private List<NoteSummaryResponse> linkedNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("isPinned")
    private boolean isPinned;

    /** AI 요약 (null = 아직 생성 안 됨) */
    private String aiSummary;

    /** 공유 중인 경우 공개 URL 토큰 (null = 비공개) */
    private String shareToken;

    /** 공유 만료 시각 (null = 만료 없음) */
    private java.time.LocalDateTime shareExpiresAt;

    @Getter
    @Builder
    public static class TagRef {
        private Long id;
        private String name;
    }
}
