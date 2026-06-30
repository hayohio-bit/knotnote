package com.knotnote.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Google Gemini 1.5 Flash 기반 AI 서비스 (OpenAI 호환 API 사용)
 *
 * 비용: 무료 티어 (15 RPM, 1M TPM, 일 1,500회 제한)
 *
 * application.yml (호환성을 위해 기존 openai 프로퍼티를 그대로 사용):
 *   openai.api-key: ${OPENAI_API_KEY:}  # 여기에 Gemini API Key를 넣으세요!
 *   openai.enabled: true
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    // Google Gemini API (OpenAI 호환 엔드포인트)
    private static final String OPENAI_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String MODEL       = "gemini-flash-latest";
    private static final int    MAX_TOKENS  = 1024;

    private final RestTemplate restTemplate;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.enabled:true}")
    private boolean enabled;

    public AiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Optional<String> summarize(String title, String content) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.debug("[AI] summarize skipped — disabled or api-key not set");
            return Optional.empty();
        }

        String prompt = buildPrompt(title, content);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", MAX_TOKENS,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                OPENAI_URL, entity, Map.class);

            if (response == null) return Optional.empty();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return Optional.empty();

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return Optional.empty();

            String summary = (String) message.get("content");
            return Optional.ofNullable(summary).map(String::trim);

        } catch (Exception e) {
            log.warn("[AI] summarize failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(String title, String content) {
        String body = content == null ? "" : content;
        // 너무 긴 본문은 앞 3000자만 전달 (토큰 절약)
        if (body.length() > 3000) body = body.substring(0, 3000) + "...";

        return """
                다음 노트를 3문장 이내로 핵심만 한국어로 요약해줘.
                불필요한 인사말 없이 요약문만 답변해.

                제목: %s

                내용:
                %s
                """.formatted(title, body);
    }
}
