package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SummarizeResponse {
    private Long noteId;
    private String summary;
    private LocalDateTime generatedAt;
}
