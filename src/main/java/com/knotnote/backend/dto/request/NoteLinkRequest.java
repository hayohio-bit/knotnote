package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NoteLinkRequest {

    @NotNull(message = "대상 메모 ID는 필수입니다")
    private Long targetNoteId;

    /** 선택 입력: 이 연결을 만드는 이유/의도 (Knot Strength 의도 명시성 차원) */
    private String intent;
}
