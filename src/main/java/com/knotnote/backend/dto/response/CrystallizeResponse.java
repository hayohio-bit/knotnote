package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CrystallizeResponse {
    private Long linkId;
    private double newStrength;
    private LocalDateTime crystallizedAt;
    private String crystallizeSummary;
}
