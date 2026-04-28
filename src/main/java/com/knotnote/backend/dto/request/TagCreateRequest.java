package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TagCreateRequest {

    @NotBlank(message = "태그 이름을 입력해주세요.")
    @Size(max = 50, message = "태그 이름은 50자 이하여야 합니다.")
    private String name;
}
