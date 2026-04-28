package com.knotnote.backend.dto.response;

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
    private boolean isPinned;
}
