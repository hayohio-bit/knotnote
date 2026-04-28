package com.knotnote.backend.repository;

import com.knotnote.backend.entity.NoteEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteEmbeddingRepository extends JpaRepository<NoteEmbedding, Long> {

    Optional<NoteEmbedding> findByNoteId(Long noteId);

    /** 사용자의 삭제되지 않은 노트 임베딩 전체 조회 (시맨틱 검색용) */
    @Query("SELECT ne FROM NoteEmbedding ne "
            + "JOIN ne.note n "
            + "WHERE n.user.id = :userId AND n.isDeleted = false")
    List<NoteEmbedding> findActiveByUserId(@Param("userId") Long userId);
}
