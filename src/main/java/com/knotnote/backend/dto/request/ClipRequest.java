package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClipRequest {

    @NotBlank(message = "URL은 필수입니다")
    @Pattern(regexp = "^https?://.*", message = "URL은 http:// 또는 https://로 시작해야 합니다")
    private String url;
}
