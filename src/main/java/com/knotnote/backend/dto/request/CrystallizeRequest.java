package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CrystallizeRequest {

    @NotBlank(message = "요약은 필수입니다")
    @Size(min = 5, message = "요약은 최소 5자 이상이어야 합니다")
    private String summary;
}
