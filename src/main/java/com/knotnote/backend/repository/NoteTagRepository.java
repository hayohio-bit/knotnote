package com.knotnote.backend.repository;

import com.knotnote.backend.entity.NoteTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteTagRepository extends JpaRepository<NoteTag, Long> {

    List<NoteTag> findByNoteId(Long noteId);

    List<NoteTag> findByTagId(Long tagId);

    Optional<NoteTag> findByNoteIdAndTagId(Long noteId, Long tagId);

    boolean existsByNoteIdAndTagId(Long noteId, Long tagId);

    /** N+1 방지: 여러 노트의 태그를 한 번에 배치 조회 */
    @Query("SELECT nt FROM NoteTag nt JOIN FETCH nt.tag WHERE nt.note.id IN :noteIds")
    List<NoteTag> findByNoteIdIn(@Param("noteIds") List<Long> noteIds);

    /** 특정 태그가 붙은 삭제되지 않은 노트 수 (태그 목록 조회용) */
    @Query("SELECT COUNT(nt) FROM NoteTag nt WHERE nt.tag.id = :tagId AND nt.note.isDeleted = false")
    long countActiveByTagId(@Param("tagId") Long tagId);
}
