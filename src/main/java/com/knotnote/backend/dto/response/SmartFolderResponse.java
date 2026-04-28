package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SmartFolderResponse {

    private Long id;
    private String name;
    private List<Long> tagIds;
    private String tagMatchMode;
    private Integer createdWithinDays;
    private String keyword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
