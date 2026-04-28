package com.knotnote.backend.repository;

import com.knotnote.backend.entity.NoteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteLinkRepository extends JpaRepository<NoteLink, Long> {

    @Query("SELECT nl FROM NoteLink nl WHERE "
            + "nl.fromNote.id = :noteId OR nl.toNote.id = :noteId")
    List<NoteLink> findAllByNoteId(@Param("noteId") Long noteId);

    /** 사용자의 모든 링크 (그래프 시각화·추천용) */
    @Query("SELECT nl FROM NoteLink nl "
            + "WHERE nl.fromNote.user.id = :userId "
            + "AND nl.fromNote.isDeleted = false AND nl.toNote.isDeleted = false")
    List<NoteLink> findAllByUserId(@Param("userId") Long userId);

    boolean existsByFromNoteIdAndToNoteId(Long fromNoteId, Long toNoteId);

    Optional<NoteLink> findByFromNoteIdAndToNoteId(Long fromNoteId, Long toNoteId);

    /** 특정 노트에 연결된 링크 수 (degree 계산용) */
    @Query("SELECT COUNT(nl) FROM NoteLink nl "
            + "WHERE nl.fromNote.id = :noteId OR nl.toNote.id = :noteId")
    long countByNoteId(@Param("noteId") Long noteId);

    /** 특정 노트의 crystallized(확정된) 링크 수 (Knot Strength linkBonus용) */
    @Query("SELECT COUNT(nl) FROM NoteLink nl "
            + "WHERE (nl.fromNote.id = :noteId OR nl.toNote.id = :noteId) "
            + "AND nl.crystallized = true")
    long countCrystallizedByNoteId(@Param("noteId") Long noteId);

    /** 사용자 전체 링크 수 (Crystallize 완성률용) */
    @Query("SELECT COUNT(nl) FROM NoteLink nl "
            + "WHERE nl.fromNote.user.id = :userId "
            + "AND nl.fromNote.isDeleted = false AND nl.toNote.isDeleted = false")
    long countByUserId(@Param("userId") Long userId);

    /** 사용자 전체 crystallized 링크 수 */
    @Query("SELECT COUNT(nl) FROM NoteLink nl "
            + "WHERE nl.fromNote.user.id = :userId "
            + "AND nl.fromNote.isDeleted = false AND nl.toNote.isDeleted = false "
            + "AND nl.crystallized = true")
    long countCrystallizedByUserId(@Param("userId") Long userId);
}
