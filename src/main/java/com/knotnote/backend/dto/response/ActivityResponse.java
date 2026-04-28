package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityResponse {

    private Long          id;
    private String        type;       // ActivityLog.ActivityType name
    private Long          noteId;
    private String        noteTitle;
    private String        detail;
    private LocalDateTime occurredAt;
}
