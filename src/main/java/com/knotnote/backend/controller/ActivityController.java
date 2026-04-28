package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.response.ActivityResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
@Tag(name = "Activity", description = "활동 피드 API")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(
            summary = "최근 활동 피드 조회",
            description = "노트 생성·수정·삭제·복원, 링크·태그 이벤트를 최신순으로 반환합니다. "
                    + "limit 기본값 30, 최대 100."
    )
    public ApiResponse<List<ActivityResponse>> getActivity(
            @RequestParam(defaultValue = "30") int limit) {
        int safeLimit = Math.min(limit, 100);
        return ApiResponse.ok(activityService.getRecentActivity(
                SecurityUtil.currentUserId(), safeLimit));
    }
}
