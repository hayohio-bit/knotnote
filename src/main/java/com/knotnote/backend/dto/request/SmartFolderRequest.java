package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class SmartFolderRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    /** 포함할 태그 ID 목록 (빈 리스트 = 태그 필터 없음) */
    private List<Long> tagIds;

    /** "ANY"(기본) | "ALL" */
    private String tagMatchMode;

    /** 최근 N일 이내 생성 (null = 제한 없음) */
    private Integer createdWithinDays;

    /** 제목·내용 포함 키워드 (null/빈 문자열 = 제한 없음) */
    private String keyword;
}
