package com.knotnote.backend.repository;

import com.knotnote.backend.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** 사용자의 최근 활동 목록 (최신순, Pageable로 limit 적용) */
    @Query("SELECT al FROM ActivityLog al WHERE al.user.id = :userId ORDER BY al.createdAt DESC")
    List<ActivityLog> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
