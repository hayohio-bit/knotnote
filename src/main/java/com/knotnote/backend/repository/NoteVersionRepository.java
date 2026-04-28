package com.knotnote.backend.repository;

import com.knotnote.backend.entity.NoteVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteVersionRepository extends JpaRepository<NoteVersion, Long> {

    /** 최신 버전 순으로 정렬하여 전체 이력 반환 */
    List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(Long noteId);

    /** 노트의 최신 버전 번호 조회 (다음 버전 번호 산출용) */
    @Query("SELECT COALESCE(MAX(nv.versionNumber), 0) FROM NoteVersion nv WHERE nv.note.id = :noteId")
    int findMaxVersionNumber(@Param("noteId") Long noteId);

    /** 특정 버전 단건 조회 (소유권 검증 포함) */
    Optional<NoteVersion> findByIdAndNoteId(Long id, Long noteId);
}
