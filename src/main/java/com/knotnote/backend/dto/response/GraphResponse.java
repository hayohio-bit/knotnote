package com.knotnote.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GraphResponse {

    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    @Getter
    @Builder
    public static class NodeDto {
        private Long id;
        private String title;
        private List<String> tags;
        private int degree;
        /** Knot Vitality Score [0.0, 1.0] → 노드 색상(초록/노랑/빨강) */
        private double vitalityScore;
    }

    @Getter
    @Builder
    public static class EdgeDto {
        private Long source;
        private Long target;
        /** Knot Strength Score [0.0, 1.0] → 엣지 두께·투명도 */
        private double strength;
        /** true=실선(확정), false=점선(임시) */
        private boolean crystallized;
        /** 연결 요약 (툴팁용) */
        private String crystallizeSummary;
        private Long linkId;
    }
}
