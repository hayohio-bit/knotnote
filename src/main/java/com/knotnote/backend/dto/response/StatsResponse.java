package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StatsResponse {

    // ── 노트 ────────────────────────────────────────────────────────
    private long totalNotes;
    /** 최근 7일 생성 노트 수 */
    private long recentNoteCount;

    // ── 링크 ────────────────────────────────────────────────────────
    private long totalLinks;
    private long crystallizedLinks;
    /** crystallizedLinks / totalLinks * 100, 링크 없으면 0.0 */
    private double crystallizationRate;

    // ── 태그 ────────────────────────────────────────────────────────
    private long totalTags;
    /** 사용 빈도 상위 5개 태그 */
    private List<TagStats> topTags;

    // ── Vitality 분포 ────────────────────────────────────────────────
    private VitalityDistribution vitalityDistribution;
    private double avgVitalityScore;

    // ── 허브 노트 ────────────────────────────────────────────────────
    /** 연결 링크 수가 가장 많은 노트 */
    private NoteSummary mostConnectedNote;

    // ── 중첩 DTO ─────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class TagStats {
        private Long   tagId;
        private String tagName;
        /** 이 태그가 붙은 삭제되지 않은 노트 수 */
        private long   noteCount;
    }

    @Getter
    @Builder
    public static class VitalityDistribution {
        /** vitalityScore >= 0.7 */
        private long high;
        /** 0.3 <= vitalityScore < 0.7 */
        private long medium;
        /** vitalityScore < 0.3 */
        private long low;
    }

    @Getter
    @Builder
    public static class NoteSummary {
        private Long   noteId;
        private String title;
        private long   linkCount;
    }
}
