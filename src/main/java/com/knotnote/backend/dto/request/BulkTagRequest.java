package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkTagRequest {

    @NotEmpty(message = "태그를 부착할 노트 ID 목록이 비어있습니다.")
    private List<Long> noteIds;

    @NotNull(message = "tagId는 필수입니다.")
    private Long tagId;
}
