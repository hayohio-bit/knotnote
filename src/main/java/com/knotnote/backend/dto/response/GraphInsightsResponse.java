package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 지식 그래프 구조 분석 결과
 *
 * - 고아 노트: 링크가 하나도 없는 노트 (연결이 필요한 후보)
 * - 허브 노트: 연결이 가장 많은 상위 노트 (핵심 개념)
 * - 약한 연결: strength 점수가 낮은 링크 (Crystallize 또는 보강이 필요한 관계)
 * - 클러스터 통계: 연결된 컴포넌트(섬) 수
 */
@Getter
@Builder
public class GraphInsightsResponse {

    /** 링크가 하나도 없는 고아 노트 목록 */
    private List<OrphanNote> orphanNotes;

    /** 연결 수 상위 5개 허브 노트 */
    private List<HubNote> hubNotes;

    /** strength < 0.3 인 약한 연결 목록 (상위 10개) */
    private List<WeakLink> weakLinks;

    /** 연결된 컴포넌트(섬) 수 — 1이면 모든 노트가 연결된 하나의 그래프 */
    private int clusterCount;

    /** 전체 노트 중 그래프에 연결된 노트 비율 [0.0, 1.0] */
    private double connectivityRate;

    /** 전체 노트 수 대비 Crystallize 완성된 링크 비율 */
    private double crystallizationRate;

    // ── 중첩 DTO ─────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class OrphanNote {
        private Long   noteId;
        private String title;
        private double vitalityScore;
    }

    @Getter
    @Builder
    public static class HubNote {
        private Long   noteId;
        private String title;
        private int    degree;          // 총 연결 수
        private long   crystallized;    // 확정된 링크 수
        private double vitalityScore;
    }

    @Getter
    @Builder
    public static class WeakLink {
        private Long   linkId;
        private Long   fromNoteId;
        private String fromTitle;
        private Long   toNoteId;
        private String toTitle;
        private double strength;
        private boolean crystallized;
    }
}
