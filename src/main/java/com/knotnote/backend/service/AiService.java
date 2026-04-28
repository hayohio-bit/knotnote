package com.knotnote.backend.service;

import java.util.Optional;

/**
 * Anthropic Claude API 연동 서비스
 * - summarize: 노트 내용을 3문장 이내로 요약
 */
public interface AiService {

    /**
     * 노트 제목 + 본문을 받아 AI 요약 생성
     *
     * @return 요약 문자열 (API 비활성화 또는 오류 시 empty)
     */
    Optional<String> summarize(String title, String content);
}
