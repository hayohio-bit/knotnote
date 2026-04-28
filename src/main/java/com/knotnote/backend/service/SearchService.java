package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.NoteSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchService {

    /** 키워드 검색 (제목 + 본문 LIKE) */
    Page<NoteSummaryResponse> search(String keyword, Long userId, Pageable pageable);

    /**
     * AI 시맨틱 검색 (임베딩 코사인 유사도 기반)
     *
     * @param query  자연어 검색어
     * @param userId 검색 대상 사용자
     * @param topN   반환할 최대 결과 수
     * @return 유사도 내림차순 정렬된 노트 목록
     */
    List<NoteSummaryResponse> semanticSearch(String query, Long userId, int topN);
}
