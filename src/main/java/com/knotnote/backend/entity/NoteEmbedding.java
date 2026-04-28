package com.knotnote.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "note_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteEmbedding {

    @Id
    private Long noteId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(columnDefinition = "LONGTEXT")
    private String embedding;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public NoteEmbedding(Note note, String embedding) {
        this.note = note;
        this.embedding = embedding;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateEmbedding(String embedding) {
        this.embedding = embedding;
        this.updatedAt = LocalDateTime.now();
    }
}
