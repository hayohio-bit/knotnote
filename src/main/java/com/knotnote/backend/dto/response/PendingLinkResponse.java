package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Crystallize Mode에서 사용하는 미확정 링크 정보
 */
@Getter
@Builder
public class PendingLinkResponse {
    private Long linkId;
    private Long fromNoteId;
    private String fromTitle;
    private Long toNoteId;
    private String toTitle;
    private double strength;
}
