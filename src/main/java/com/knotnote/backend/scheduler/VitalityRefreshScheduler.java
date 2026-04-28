package com.knotnote.backend.scheduler;

import com.knotnote.backend.repository.UserRepository;
import com.knotnote.backend.service.KnotVitalityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Knot Vitality Score 자동 갱신 스케줄러
 *
 * 매일 새벽 3시(서버 타임존 기준)에 전체 사용자의 Vitality Score를 재계산·저장합니다.
 * - 시간 감쇠(decay) 항이 날마다 변하므로 DB 저장값을 최신 상태로 유지
 * - 실시간 계산(calculateVitality)은 여전히 API 응답에서도 별도로 수행 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VitalityRefreshScheduler {

    private final UserRepository      userRepository;
    private final KnotVitalityService knotVitalityService;

    /**
     * 매일 03:00 (KST) 전체 사용자 Vitality 일괄 갱신
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void refreshAllUsersVitality() {
        long start = System.currentTimeMillis();
        log.info("[VitalityScheduler] 전체 사용자 Vitality 갱신 시작");

        long userCount = 0;
        try {
            // 전체 사용자 ID 스트림 처리 (메모리 효율)
            var users = userRepository.findAll();
            userCount = users.size();

            for (var user : users) {
                try {
                    knotVitalityService.refreshVitalityScores(user.getId());
                } catch (Exception e) {
                    // 사용자 한 명 실패해도 나머지는 계속 처리
                    log.warn("[VitalityScheduler] userId={} 갱신 실패: {}", user.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[VitalityScheduler] 스케줄러 전체 실패", e);
            return;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("[VitalityScheduler] 완료 — 사용자 {}명, 소요 {}ms", userCount, elapsed);
    }
}
