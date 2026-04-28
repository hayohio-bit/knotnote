package com.knotnote.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스마트 폴더 — 사용자가 저장해두는 동적 필터 프리셋.
 * 실제 메모를 저장하지 않고, 조건에 맞는 메모를 조회할 때마다 실시간으로 집계한다.
 *
 * 필터 조건:
 *  - tagIds        : 쉼표 구분 tagId 목록 (ex. "1,3,7"). 빈 문자열 = 태그 필터 없음.
 *  - tagMatchMode  : ANY(하나라도 포함) | ALL(모두 포함)
 *  - createdWithin : 최근 N일 이내 생성 (null = 기간 제한 없음)
 *  - keyword       : 제목·내용 포함 키워드 (null/빈 문자열 = 제한 없음)
 */
@Entity
@Table(name = "smart_folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmartFolder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    /** 쉼표 구분 tagId 문자열 */
    @Column(length = 500)
    private String tagIds;

    /** "ANY" or "ALL" */
    @Column(nullable = false, length = 10)
    private String tagMatchMode;

    /** null = 기간 제한 없음 */
    @Column
    private Integer createdWithinDays;

    @Column(length = 200)
    private String keyword;

    @Builder
    public SmartFolder(User user, String name, String tagIds,
                       String tagMatchMode, Integer createdWithinDays, String keyword) {
        this.user = user;
        this.name = name;
        this.tagIds = tagIds == null ? "" : tagIds;
        this.tagMatchMode = tagMatchMode == null ? "ANY" : tagMatchMode;
        this.createdWithinDays = createdWithinDays;
        this.keyword = keyword;
    }

    public void update(String name, String tagIds, String tagMatchMode,
                       Integer createdWithinDays, String keyword) {
        this.name = name;
        this.tagIds = tagIds == null ? "" : tagIds;
        this.tagMatchMode = tagMatchMode == null ? "ANY" : tagMatchMode;
        this.createdWithinDays = createdWithinDays;
        this.keyword = keyword;
    }
}
