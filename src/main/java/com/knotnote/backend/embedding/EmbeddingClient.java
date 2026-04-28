package com.knotnote.backend.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Python 임베딩 서버 HTTP 클라이언트
 *
 * - embed.server.enabled=false 이면 모든 호출을 건너뜀 (테스트 환경)
 * - 서버가 다운되거나 오류가 발생해도 Optional.empty() 반환 (fail-safe)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final RestTemplate restTemplate;

    @Value("${embed.server.url:http://localhost:8000}")
    private String serverUrl;

    @Value("${embed.server.enabled:true}")
    private boolean enabled;

    /**
     * 텍스트를 임베딩 벡터로 변환
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터 (서버 비활성화 또는 오류 시 Optional.empty())
     */
    public Optional<List<Double>> embed(String text) {
        if (!enabled) {
            log.debug("Embedding server disabled — skipping embed call");
            return Optional.empty();
        }
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            EmbedResponse response = restTemplate.postForObject(
                    serverUrl + "/embed",
                    new EmbedRequest(text),
                    EmbedResponse.class
            );
            if (response == null || response.getEmbedding() == null) {
                log.warn("Embedding server returned null response");
                return Optional.empty();
            }
            return Optional.of(response.getEmbedding());
        } catch (Exception e) {
            log.warn("Embedding server call failed (embed): {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 서버 헬스 체크
     */
    public boolean isHealthy() {
        if (!enabled) return false;
        try {
            restTemplate.getForObject(serverUrl + "/health", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
