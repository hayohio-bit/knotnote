package com.knotnote.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knotnote.backend.embedding.EmbeddingClient;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteEmbedding;
import com.knotnote.backend.repository.NoteEmbeddingRepository;
import com.knotnote.backend.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingClient embeddingClient;
    private final NoteEmbeddingRepository noteEmbeddingRepository;
    private final NoteRepository noteRepository;
    private final ObjectMapper objectMapper;

    // ── 비동기 인덱싱 ──────────────────────────────────────────────────────

    @Override
    @Async("embeddingExecutor")
    @Transactional
    public void indexNoteAsync(Note note) {
        String text = buildText(note);
        embeddingClient.embed(text).ifPresent(vec -> {
            try {
                String json = objectMapper.writeValueAsString(vec);
                noteEmbeddingRepository.findByNoteId(note.getId()).ifPresentOrElse(
                        existing -> existing.updateEmbedding(json),
                        () -> noteEmbeddingRepository.save(
                                NoteEmbedding.builder().note(noteRepository.getReferenceById(note.getId())).embedding(json).build())
                );
                log.debug("Indexed embedding for noteId={}", note.getId());
            } catch (Exception e) {
                log.warn("Failed to save embedding for noteId={}: {}", note.getId(), e.getMessage());
            }
        });
    }

    // ── 조회 ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<double[]> getEmbedding(Long noteId) {
        return noteEmbeddingRepository.findByNoteId(noteId)
                .map(ne -> parseEmbedding(ne.getEmbedding()));
    }

    // ── 쿼리 임베딩 ────────────────────────────────────────────────────────

    @Override
    public Optional<double[]> embedText(String text) {
        return embeddingClient.embed(text)
                .map(vec -> vec.stream().mapToDouble(Double::doubleValue).toArray());
    }

    // ── 웹 클리핑 ─────────────────────────────────────────────────────────

    @Override
    public Optional<EmbeddingClient.ClipResult> clip(String url) {
        return embeddingClient.clip(url);
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────────────

    /**
     * 노트 제목 + 본문을 하나의 문자열로 합침
     * 제목에 가중치를 주기 위해 제목을 2회 포함
     */
    private String buildText(Note note) {
        String title   = note.getTitle()   == null ? "" : note.getTitle();
        String content = note.getContent() == null ? "" : note.getContent();
        return title + " " + title + " " + content;
    }

    private double[] parseEmbedding(String json) {
        try {
            List<Double> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream().mapToDouble(Double::doubleValue).toArray();
        } catch (Exception e) {
            log.warn("Failed to parse embedding JSON: {}", e.getMessage());
            return new double[0];
        }
    }
}
