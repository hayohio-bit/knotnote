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

    @Getter
    @Builder
    public static class TagRef {
        private Long id;
        private String name;
    }
}
