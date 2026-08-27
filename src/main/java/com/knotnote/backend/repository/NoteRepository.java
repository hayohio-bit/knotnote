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

    /** 공유 토큰으로 노트 조회 (공개 접근용) */
    Optional<Note> findByShareToken(String shareToken);

    /** 태그 필터 — ANY: 지정한 태그 중 하나라도 달린 노트 */
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND n.isDeleted = false "
            + "AND n.id IN (SELECT nt.note.id FROM NoteTag nt WHERE nt.tag.id IN :tagIds)")
    Page<Note> findByUserIdAndAnyTag(@Param("userId") Long userId,
                                     @Param("tagIds") List<Long> tagIds,
                                     Pageable pageable);

    /** 태그 필터 — ALL: 지정한 태그가 전부 달린 노트 */
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND n.isDeleted = false "
            + "AND n.id IN (SELECT nt.note.id FROM NoteTag nt WHERE nt.tag.id IN :tagIds "
            + "GROUP BY nt.note.id HAVING COUNT(DISTINCT nt.tag.id) = :tagCount)")
    Page<Note> findByUserIdAndAllTags(@Param("userId") Long userId,
                                      @Param("tagIds") List<Long> tagIds,
                                      @Param("tagCount") long tagCount,
                                      Pageable pageable);

    /** 상단 고정 노트 전체 (사이드바용) */
    List<Note> findByUserIdAndIsDeletedFalseAndIsPinnedTrueOrderByUpdatedAtDesc(Long userId);
}
