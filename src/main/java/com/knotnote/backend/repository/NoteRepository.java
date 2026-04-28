package com.knotnote.backend.repository;

import com.knotnote.backend.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Page<Note> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    List<Note> findByUserIdAndIsDeletedFalse(Long userId);

    Optional<Note> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId "
            + "AND n.isDeleted = false "
            + "AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)")
    Page<Note> searchByKeyword(@Param("userId") Long userId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /** low-vitality 전용: vitalityScore threshold 미만만 조회 */
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND n.isDeleted = false "
            + "AND n.vitalityScore < :threshold "
            + "ORDER BY n.vitalityScore ASC")
    List<Note> findLowVitality(@Param("userId") Long userId,
                               @Param("threshold") double threshold);
}
