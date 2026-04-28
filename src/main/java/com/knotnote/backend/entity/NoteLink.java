package com.knotnote.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "note_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_note_id", "to_note_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_note_id", nullable = false)
    private Note fromNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_note_id", nullable = false)
    private Note toNote;

    /** 연결 의도 메모 (Knot Strength 의도 명시성 차원) */
    @Column(columnDefinition = "TEXT")
    private String intent;

    /** Crystallize Mode: 사용자가 요약을 완성했는지 여부 */
    @Column(nullable = false)
    private boolean crystallized = false;

    /** Crystallize Mode: 사용자가 입력한 연결 관계 요약 */
    @Column(columnDefinition = "TEXT")
    private String crystallizeSummary;

    /** Crystallize Mode: 확정된 시각 */
    private LocalDateTime crystallizedAt;

    /** Knot Strength Score [0.0, 1.0] — Phase 1: Jaccard + intent (최대 0.4) */
    @Column(nullable = false)
    private double strength = 0.0;

    @Builder
    public NoteLink(Note fromNote, Note toNote, String intent) {
        this.fromNote = fromNote;
        this.toNote = toNote;
        this.intent = intent;
    }

    /** Crystallize 확정 처리 */
    public void crystallize(String summary) {
        this.crystallized = true;
        this.crystallizeSummary = summary.trim();
        this.crystallizedAt = LocalDateTime.now();
    }

    /** Strength 점수 갱신 */
    public void updateStrength(double score) {
        this.strength = Math.min(Math.max(score, 0.0), 1.0);
    }

    /** intent 업데이트 */
    public void updateIntent(String intent) {
        this.intent = intent;
    }
}
