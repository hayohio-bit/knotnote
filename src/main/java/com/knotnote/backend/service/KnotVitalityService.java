package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.DecayAlertResponse;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteLink;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.repository.NoteLinkRepository;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Knot Vitality Score & Knot Strength Score 계산 서비스
 *
 * [Knot Vitality Score]
 *   = clamp(e^(-λ * daysSinceUpdate) + linkBonus + editBonus, 0.0, 1.0)
 *   - λ = 0.05  → 약 14일 후 decay 항이 0.5로 하락
 *   - linkBonus = min(crystallizedLinks * 0.1, 0.5)
 *   - editBonus = 0.2 if updatedAt > createdAt + 1min (수정 이력 있음), else 0
 *
 * [Knot Strength Score — Phase 1 (규칙 기반, 최대 0.4)]
 *   = structuralSim * 0.2  (Jaccard 태그 유사도)
 *   + intentScore   * 0.2  (intent 입력 여부)
 *   + 0.0           * 0.2  (행동 빈도 — Phase 2)
 *   + 0.0           * 0.4  (임베딩 유사도 — Phase 2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnotVitalityService {

    private static final double DECAY_LAMBDA = 0.05;
    private static final double VITALITY_ALERT_THRESHOLD = 0.3;
    private static final int    MIN_DEGREE_FOR_ALERT     = 2;
    private static final int    MAX_ALERTS               = 5;

    private final NoteRepository     noteRepository;
    private final NoteLinkRepository noteLinkRepository;
    private final NoteTagRepository  noteTagRepository;
    private final EmbeddingService   embeddingService;

    // ── Vitality Score ─────────────────────────────────────────────

    /**
     * 단일 노트의 Vitality Score 계산 (실시간, 저장하지 않음)
     */
    public double calculateVitality(Note note) {
        // ① 시간 감쇠: updatedAt을 마지막 접근 시간 프록시로 사용
        long daysSince = ChronoUnit.DAYS.between(
                note.getUpdatedAt(), LocalDateTime.now());
        double decayScore = Math.exp(-DECAY_LAMBDA * daysSince);

        // ② Crystallized 링크 수 기반 linkBonus
        long crystallizedLinks = noteLinkRepository.countCrystallizedByNoteId(note.getId());
        double linkBonus = Math.min(crystallizedLinks * 0.1, 0.5);

        // ③ 수정 이력 보너스: createdAt 이후 1분 이상 지나서 수정된 경우
        double editBonus = 0.0;
        if (note.getCreatedAt() != null && note.getUpdatedAt() != null) {
            long minutesSinceCreate = ChronoUnit.MINUTES.between(
                    note.getCreatedAt(), note.getUpdatedAt());
            if (minutesSinceCreate > 1) {
                editBonus = 0.2;
            }
        }

        return Math.min(decayScore + linkBonus + editBonus, 1.0);
    }

    /**
     * 사용자의 모든 노트 Vitality Score를 일괄 계산하고 DB에 저장
     */
    @Transactional
    public void refreshVitalityScores(Long userId) {
        List<Note> notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        notes.forEach(note -> {
            double score = calculateVitality(note);
            note.updateVitalityScore(score);
        });
        noteRepository.saveAll(notes);
        log.debug("Vitality scores refreshed for userId={}, count={}", userId, notes.size());
    }

    // ── Decay Alerts ───────────────────────────────────────────────

    /**
     * Knot Decay 알림 생성
     * 조건: vitalityScore < 0.3 AND degree >= 2 (허브 노트만)
     */
    @Transactional(readOnly = true)
    public List<DecayAlertResponse> generateDecayAlerts(Long userId) {
        // 최신 Vitality를 실시간 계산해서 반환 (DB 저장값과 별개)
        List<Note> notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);

        return notes.stream()
                .map(note -> {
                    double vitality = calculateVitality(note);
                    long degree = noteLinkRepository.countByNoteId(note.getId());
                    long pending = noteLinkRepository.countByNoteId(note.getId())
                            - noteLinkRepository.countCrystallizedByNoteId(note.getId());
                    return Map.entry(note, new double[]{vitality, degree, pending});
                })
                .filter(e -> e.getValue()[0] < VITALITY_ALERT_THRESHOLD
                        && e.getValue()[1] >= MIN_DEGREE_FOR_ALERT)
                .sorted(Comparator.comparingDouble((Map.Entry<Note, double[]> e) -> e.getValue()[0]))
                .limit(MAX_ALERTS)
                .map(e -> DecayAlertResponse.builder()
                        .noteId(e.getKey().getId())
                        .noteTitle(e.getKey().getTitle())
                        .vitalityScore(Math.round(e.getValue()[0] * 1000.0) / 1000.0)
                        .connectedNoteCount((long) e.getValue()[1])
                        .pendingCrystallizeCount((long) e.getValue()[2])
                        .build())
                .collect(Collectors.toList());
    }

    // ── Knot Strength Score ────────────────────────────────────────

    /**
     * 단일 링크의 Strength Score 계산 (Phase 1 규칙 기반)
     * Phase 2에서 임베딩 코사인 유사도(0.4) 추가 예정
     */
    public double calculateStrength(NoteLink link) {
        Note source = link.getFromNote();
        Note target = link.getToNote();

        // ① 구조적 유사도: 공유 태그 Jaccard
        List<Long> sourceIds = List.of(source.getId());
        List<Long> targetIds = List.of(target.getId());

        Set<Long> srcTagIds = noteTagRepository.findByNoteIdIn(sourceIds).stream()
                .map(nt -> nt.getTag().getId()).collect(Collectors.toSet());
        Set<Long> tgtTagIds = noteTagRepository.findByNoteIdIn(targetIds).stream()
                .map(nt -> nt.getTag().getId()).collect(Collectors.toSet());

        double structuralSim = jaccardSimilarity(srcTagIds, tgtTagIds);

        // ② 의도 명시성: intent 입력 여부
        double intentScore = (link.getIntent() != null && !link.getIntent().isBlank()) ? 1.0 : 0.0;

        // ③ 행동 빈도: Phase 2 (현재 0)
        double behaviorScore = 0.0;

        // ④ 의미 유사도: Phase 3 — 임베딩 코사인 유사도
        double semanticSim = calculateSemanticSimilarity(source, target);

        double raw = structuralSim * 0.2
                + intentScore   * 0.2
                + behaviorScore * 0.2
                + semanticSim   * 0.4;

        return Math.min(raw, 1.0);
    }

    /**
     * 두 노트의 임베딩 코사인 유사도 계산 (Phase 3)
     * - 임베딩이 없거나 서버가 비활성화된 경우 0.0 반환 (Phase 1 동작으로 폴백)
     */
    private double calculateSemanticSimilarity(Note source, Note target) {
        Optional<double[]> srcVec = embeddingService.getEmbedding(source.getId());
        Optional<double[]> tgtVec = embeddingService.getEmbedding(target.getId());
        if (srcVec.isEmpty() || tgtVec.isEmpty()) return 0.0;
        return cosineSimilarity(srcVec.get(), tgtVec.get());
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static <T> double jaccardSimilarity(Set<T> a, Set<T> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        long intersection = a.stream().filter(b::contains).count();
        long union = (long) a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }
}
