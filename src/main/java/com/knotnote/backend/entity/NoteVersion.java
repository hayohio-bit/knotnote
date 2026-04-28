package com.knotnote.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 노트 수정 이력 스냅샷
 *
 * 노트가 수정될 때마다 변경 전 상태를 여기에 저장합니다.
 * 최신 내용은 Note 테이블이 정본이며, NoteVersion은 과거 이력입니다.
 */
@Entity
@Table(name = "note_versions",
        indexes = @Index(name = "idx_note_version_note_id", columnList = "note_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    /** 이 버전의 제목 스냅샷 */
    @Column(nullable = false)
    private String title;

    /** 이 버전의 본문 스냅샷 */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /** 버전 순번 (노트 내에서 1부터 증가) */
    @Column(nullable = false)
    private int versionNumber;

    /** 스냅샷 생성 시각 (= 수정이 발생한 시각) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public NoteVersion(Note note, String title, String content, int versionNumber) {
        this.note          = note;
        this.title         = title;
        this.content       = content;
        this.versionNumber = versionNumber;
    }
}
