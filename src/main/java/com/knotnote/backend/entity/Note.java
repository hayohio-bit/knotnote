package com.knotnote.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private boolean isDeleted = false;

    /**
     * Knot Vitality Score [0.0, 1.0]
     * = e^(-λ * daysSinceUpdate) + linkBonus + editBonus
     * 기본값 0.5 (신규 노트)
     */
    @Column(nullable = false)
    private double vitalityScore = 0.5;

    /** Phase 5: 상단 고정 여부 */
    @Column(nullable = false)
    private boolean isPinned = false;

    @Builder
    public Note(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.isDeleted = false;
        this.vitalityScore = 0.5;
        this.isPinned = false;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void updateVitalityScore(double score) {
        this.vitalityScore = Math.min(Math.max(score, 0.0), 1.0);
    }

    public void pin()   { this.isPinned = true; }
    public void unpin() { this.isPinned = false; }
}
