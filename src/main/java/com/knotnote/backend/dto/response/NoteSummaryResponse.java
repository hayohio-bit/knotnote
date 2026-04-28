package com.knotnote.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NoteSummaryResponse {
    private Long id;
    private String title;
    private String preview;
    private List<NoteDetailResponse.TagRef> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("isPinned")
    private boolean isPinned;
}
