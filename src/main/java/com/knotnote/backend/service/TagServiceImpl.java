package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.TagCreateRequest;
import com.knotnote.backend.dto.response.TagResponse;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.entity.Tag;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import com.knotnote.backend.repository.TagRepository;
import com.knotnote.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final NoteRepository noteRepository;
    private final NoteTagRepository noteTagRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getTags(Long userId) {
        return tagRepository.findByUserId(userId).stream()
                .map(tag -> TagResponse.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .noteCount(noteTagRepository.countActiveByTagId(tag.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public TagResponse createTag(TagCreateRequest request, Long userId) {
        if (tagRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new CustomException(ErrorCode.DUPLICATE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        Tag tag = Tag.builder()
                .user(user)
                .name(request.getName())
                .build();

        tagRepository.save(tag);

        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .noteCount(0)
                .build();
    }

    @Override
    public void deleteTag(Long tagId, Long userId) {
        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 관련 NoteTag 매핑 먼저 삭제
        List<NoteTag> links = noteTagRepository.findByTagId(tagId);
        noteTagRepository.deleteAll(links);

        tagRepository.delete(tag);
    }

    @Override
    public void addTagToNote(Long noteId, Long tagId, Long userId) {
        Note note = noteRepository.findByIdAndUserIdAndIsDeletedFalse(noteId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
            throw new CustomException(ErrorCode.DUPLICATE);
        }

        noteTagRepository.save(NoteTag.builder()
                .note(note)
                .tag(tag)
                .build());
    }

    @Override
    public void removeTagFromNote(Long noteId, Long tagId, Long userId) {
        noteRepository.findByIdAndUserIdAndIsDeletedFalse(noteId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        NoteTag noteTag = noteTagRepository
                .findByNoteIdAndTagId(noteId, tagId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        noteTagRepository.delete(noteTag);
    }
}
