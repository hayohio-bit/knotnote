package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.NoteDetailResponse;
import com.knotnote.backend.dto.response.NoteSummaryResponse;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteEmbedding;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.repository.NoteEmbeddingRepository;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final int PREVIEW_LENGTH = 500;
    private static final double SEMANTIC_THRESHOLD = 0.3; // 최소 유사도 필터

    private final NoteRepository         noteRepository;
    private final NoteTagRepository      noteTagRepository;
    private final NoteEmbeddingRepository noteEmbeddingRepository;
    private final EmbeddingService        embeddingService;

    @Override
    public Page<NoteSummaryResponse> search(String keyword, Long userId, Pageable pageable) {
        Page<Note> page = noteRepository.searchByKeyword(userId, keyword, pageable);

        // N+1 방지: 페이지 내 모든 노트의 태그를 한 번에 배치 조회
        List<Long> noteIds = page.getContent().stream()
                .map(Note::getId)
                .collect(Collectors.toList());

        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(noteIds);

        return page.map(note -> {
            String preview = note.getContent() != null && note.getContent().length() > PREVIEW_LENGTH
                    ? note.getContent().substring(0, PREVIEW_LENGTH)
                    : note.getContent();

            return NoteSummaryResponse.builder()
                    .id(note.getId())
                    .title(note.getTitle())
                    .preview(preview)
                    .tags(tagMap.getOrDefault(note.getId(), List.of()))
                    .createdAt(note.getCreatedAt())
                    .updatedAt(note.getUpdatedAt())
                    .isPinned(note.isPinned())
                    .build();
        });
    }

    // ── 시맨틱 검색 ────────────────────────────────────────────────────────

    @Override
    public List<NoteSummaryResponse> semanticSearch(String query, Long userId, int topN) {
        // 1. 쿼리 임베딩
        Optional<double[]> queryVecOpt = embeddingService.embedText(query);
        if (queryVecOpt.isEmpty()) {
            log.warn("Semantic search skipped — embedding server unavailable or disabled");
            return List.of();
        }
        double[] queryVec = queryVecOpt.get();

        // 2. 유저의 노트 임베딩 전체 조회
        List<NoteEmbedding> embeddings = noteEmbeddingRepository.findActiveByUserId(userId);
        if (embeddings.isEmpty()) {
            return List.of();
        }

        // 3. 태그 맵 구성 (N+1 방지)
        List<Long> noteIds = embeddings.stream()
                .map(ne -> ne.getNote().getId())
                .collect(Collectors.toList());
        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(noteIds);

        // 4. 코사인 유사도 계산 후 정렬
        return embeddings.stream()
                .map(ne -> {
                    double[] noteVec = parseEmbedding(ne.getEmbedding());
                    double score = (noteVec.length == 0) ? 0.0 : cosineSimilarity(queryVec, noteVec);
                    return Map.entry(ne.getNote(), score);
                })
                .filter(e -> e.getValue() >= SEMANTIC_THRESHOLD)
                .sorted(Map.Entry.<Note, Double>comparingByValue().reversed())
                .limit(topN)
                .map(e -> {
                    Note note = e.getKey();
                    String preview = note.getContent() != null
                            && note.getContent().length() > PREVIEW_LENGTH
                            ? note.getContent().substring(0, PREVIEW_LENGTH)
                            : note.getContent();
                    return NoteSummaryResponse.builder()
                            .id(note.getId())
                            .title(note.getTitle())
                            .preview(preview)
                            .tags(tagMap.getOrDefault(note.getId(), List.of()))
                            .createdAt(note.getCreatedAt())
                            .updatedAt(note.getUpdatedAt())
                            .isPinned(note.isPinned())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────────────

    private Map<Long, List<NoteDetailResponse.TagRef>> buildTagMap(List<Long> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        return noteTagRepository.findByNoteIdIn(noteIds).stream()
                .collect(Collectors.groupingBy(
                        nt -> nt.getNote().getId(),
                        Collectors.mapping(
                                nt -> NoteDetailResponse.TagRef.builder()
                                        .id(nt.getTag().getId())
                                        .name(nt.getTag().getName())
                                        .build(),
                                Collectors.toList()
                        )
                ));
    }

    /** JSON 문자열 → double 배열 */
    private double[] parseEmbedding(String json) {
        try {
            // 간단한 JSON 배열 파싱: "[0.1, 0.2, ...]"
            String trimmed = json.trim().replaceAll("[\\[\\]]", "");
            if (trimmed.isEmpty()) return new double[0];
            String[] parts = trimmed.split(",");
            double[] vec = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vec[i] = Double.parseDouble(parts[i].trim());
            }
            return vec;
        } catch (Exception e) {
            log.warn("Failed to parse embedding: {}", e.getMessage());
            return new double[0];
        }
    }

    /**
     * 코사인 유사도 (L2 정규화된 벡터 기준 내적과 동일)
     * Python 서버에서 normalize_embeddings=True 로 저장했으므로 내적으로 계산 가능
     */
    private static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) return 0.0;
        double dot  = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot  += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
