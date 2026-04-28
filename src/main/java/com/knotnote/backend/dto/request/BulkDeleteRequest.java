package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkDeleteRequest {

    @NotEmpty(message = "삭제할 노트 ID 목록이 비어있습니다.")
    private List<Long> noteIds;
}
