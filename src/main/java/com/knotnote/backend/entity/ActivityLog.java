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
 * 사용자의 지식 그래프 활동 이력
 *
 * 이벤트 유형(ActivityType):
 *   NOTE_CREATED, NOTE_UPDATED, NOTE_DELETED, NOTE_RESTORED,
 *   NOTE_PINNED, NOTE_UNPINNED,
 *   LINK_CREATED, LINK_DELETED, LINK_CRYSTALLIZED,
 *   TAG_ADDED, TAG_REMOVED
 */
@Entity
@Table(name = "activity_logs",
        indexes = {
            @Index(name = "idx_activity_user_id", columnList = "user_id"),
            @Index(name = "idx_activity_created_at", columnList = "created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    /** 이벤트가 발생한 노트 ID (링크·태그 이벤트도 기준 노트 기록) */
    @Column(name = "note_id")
    private Long noteId;

    /** 이벤트 발생 시 노트 제목 스냅샷 */
    @Column(name = "note_title")
    private String noteTitle;

    /** 추가 컨텍스트: 링크 대상 노트 ID, 태그 이름 등 (선택) */
    @Column(length = 255)
    private String detail;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ActivityLog(User user, ActivityType type, Long noteId, String noteTitle, String detail) {
        this.user      = user;
        this.type      = type;
        this.noteId    = noteId;
        this.noteTitle = noteTitle;
        this.detail    = detail;
    }

    public enum ActivityType {
        NOTE_CREATED, NOTE_UPDATED, NOTE_DELETED, NOTE_RESTORED,
        NOTE_PINNED, NOTE_UNPINNED,
        LINK_CREATED, LINK_DELETED, LINK_CRYSTALLIZED,
        TAG_ADDED, TAG_REMOVED
    }
}
