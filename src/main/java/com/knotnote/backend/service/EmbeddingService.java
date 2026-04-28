package com.knotnote.backend.service;

import com.knotnote.backend.embedding.EmbeddingClient;
import com.knotnote.backend.entity.Note;

import java.util.Optional;

public interface EmbeddingService {

    /**
     * 노트를 비동기로 임베딩 인덱싱 (생성·수정 시 호출)
     * - 임베딩 서버가 다운된 경우 조용히 실패
     */
    void indexNoteAsync(Note note);

    /**
     * 노트의 임베딩 벡터를 동기적으로 조회
     * - DB에 저장된 값 반환 (없으면 Optional.empty())
     */
    Optional<double[]> getEmbedding(Long noteId);

    /**
     * 텍스트를 즉시 임베딩하여 벡터 반환 (검색 쿼리 임베딩용)
     */
    Optional<double[]> embedText(String text);

    /**
     * URL에서 제목·본문 추출 (웹 클리핑)
     * - embed_server /clip 엔드포인트 호출
     */
    Optional<EmbeddingClient.ClipResult> clip(String url);
}
