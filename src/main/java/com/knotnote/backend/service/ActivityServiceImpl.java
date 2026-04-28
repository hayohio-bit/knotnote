package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.ActivityResponse;
import com.knotnote.backend.entity.ActivityLog;
import com.knotnote.backend.entity.ActivityLog.ActivityType;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getRecentActivity(Long userId, int limit) {
        return activityLogRepository
                .findRecentByUserId(userId, PageRequest.of(0, limit))
                .stream()
                .map(al -> ActivityResponse.builder()
                        .id(al.getId())
                        .type(al.getType().name())
                        .noteId(al.getNoteId())
                        .noteTitle(al.getNoteTitle())
                        .detail(al.getDetail())
                        .occurredAt(al.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void record(User user, ActivityType type, Note note, String detail) {
        activityLogRepository.save(ActivityLog.builder()
                .user(user)
                .type(type)
                .noteId(note != null ? note.getId() : null)
                .noteTitle(note != null ? note.getTitle() : null)
                .detail(detail)
                .build());
    }
}
