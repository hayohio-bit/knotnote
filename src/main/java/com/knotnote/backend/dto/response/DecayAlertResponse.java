package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DecayAlertResponse {
    private Long noteId;
    private String noteTitle;
    private double vitalityScore;
    private long connectedNoteCount;
    /** 미확정(점선) 링크 수 */
    private long pendingCrystallizeCount;
}
