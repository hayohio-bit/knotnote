package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.ActivityResponse;
import com.knotnote.backend.entity.ActivityLog.ActivityType;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.User;

import java.util.List;

public interface ActivityService {

    /** 최근 활동 목록 조회 (최신순) */
    List<ActivityResponse> getRecentActivity(Long userId, int limit);

    /** 활동 기록 저장 (서비스 레이어에서 직접 호출) */
    void record(User user, ActivityType type, Note note, String detail);

    /** detail 없이 기록 */
    default void record(User user, ActivityType type, Note note) {
        record(user, type, note, null);
    }
}
