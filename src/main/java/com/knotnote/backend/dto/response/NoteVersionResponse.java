package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoteVersionResponse {

    private Long          versionId;
    private int           versionNumber;
    private String        title;
    private String        content;
    /** 이 버전이 저장된 시각 (= 해당 수정이 발생한 시각) */
    private LocalDateTime savedAt;
}
