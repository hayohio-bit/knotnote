package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.SmartFolderRequest;
import com.knotnote.backend.dto.response.NoteDetailResponse;
import com.knotnote.backend.dto.response.NoteSummaryResponse;
import com.knotnote.backend.dto.response.SmartFolderResponse;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.entity.SmartFolder;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import com.knotnote.backend.repository.SmartFolderRepository;
import com.knotnote.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SmartFolderServiceImpl implements SmartFolderService {

    private static final int PREVIEW_LENGTH = 500;

    private final SmartFolderRepository smartFolderRepository;
    private final NoteRepository noteRepository;
    private final NoteTagRepository noteTagRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SmartFolderResponse> list(Long userId) {
        return smartFolderRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public SmartFolderResponse create(SmartFolderRequest req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        SmartFolder folder = SmartFolder.builder()
                .user(user)
                .name(req.getName())
                .tagIds(joinTagIds(req.getTagIds()))
                .tagMatchMode(req.getTagMatchMode())
                .createdWithinDays(req.getCreatedWithinDays())
                .keyword(req.getKeyword())
                .build();
        return toResponse(smartFolderRepository.save(folder));
    }

    @Override
    public SmartFolderResponse update(Long folderId, SmartFolderRequest req, Long userId) {
        SmartFolder folder = findOrThrow(folderId, userId);
        folder.update(req.getName(), joinTagIds(req.getTagIds()),
                req.getTagMatchMode(), req.getCreatedWithinDays(), req.getKeyword());
        return toResponse(folder);
    }

    @Override
    public void delete(Long folderId, Long userId) {
        SmartFolder folder = findOrThrow(folderId, userId);
        smartFolderRepository.delete(folder);
    }

    /**
     * 스마트 폴더 조건에 맞는 노트 목록 반환 (실시간 집계)
     *
     * 필터 우선순위: tagIds → keyword → createdWithinDays
     * 각 조건은 AND로 결합됨.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getNotes(Long folderId, Long userId) {
        SmartFolder folder = findOrThrow(folderId, userId);

        List<Note> allNotes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Long> allIds = allNotes.stream().map(Note::getId).collect(Collectors.toList());

        // ── 태그 맵 구성 (noteId → Set<tagId>) ──
        Map<Long, Set<Long>> tagIdMap = new HashMap<>();
        if (!allIds.isEmpty()) {
            noteTagRepository.findByNoteIdIn(allIds).forEach(nt ->
                    tagIdMap.computeIfAbsent(nt.getNote().getId(), k -> new HashSet<>())
                            .add(nt.getTag().getId()));
        }

        // ── 태그 Ref 맵 (응답용) ──
        Map<Long, List<NoteDetailResponse.TagRef>> tagRefMap = new HashMap<>();
        if (!allIds.isEmpty()) {
            noteTagRepository.findByNoteIdIn(allIds).forEach(nt ->
                    tagRefMap.computeIfAbsent(nt.getNote().getId(), k -> new ArrayList<>())
                            .add(NoteDetailResponse.TagRef.builder()
                                    .id(nt.getTag().getId()).name(nt.getTag().getName()).build()));
        }

        // ── 필터 조건 파싱 ──
        List<Long> requiredTagIds = parseTagIds(folder.getTagIds());
        boolean useTagFilter = !requiredTagIds.isEmpty();
        boolean isAllMode = "ALL".equalsIgnoreCase(folder.getTagMatchMode());
        String kw = folder.getKeyword() == null ? "" : folder.getKeyword().trim().toLowerCase();
        boolean useKeyword = !kw.isEmpty();
        Integer withinDays = folder.getCreatedWithinDays();
        LocalDateTime cutoff = withinDays != null
                ? LocalDateTime.now().minusDays(withinDays) : null;

        return allNotes.stream()
                .filter(note -> {
                    // 태그 필터
                    if (useTagFilter) {
                        Set<Long> noteTags = tagIdMap.getOrDefault(note.getId(), Set.of());
                        if (isAllMode) {
                            if (!noteTags.containsAll(requiredTagIds)) return false;
                        } else {
                            if (requiredTagIds.stream().noneMatch(noteTags::contains)) return false;
                        }
                    }
                    // 키워드 필터
                    if (useKeyword) {
                        String title = note.getTitle() == null ? "" : note.getTitle().toLowerCase();
                        String content = note.getContent() == null ? "" : note.getContent().toLowerCase();
                        if (!title.contains(kw) && !content.contains(kw)) return false;
                    }
                    // 날짜 필터
                    if (cutoff != null && note.getCreatedAt().isBefore(cutoff)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(Note::getCreatedAt).reversed())
                .map(note -> {
                    String preview = note.getContent() != null
                            && note.getContent().length() > PREVIEW_LENGTH
                            ? note.getContent().substring(0, PREVIEW_LENGTH)
                            : note.getContent();
                    return NoteSummaryResponse.builder()
                            .id(note.getId()).title(note.getTitle()).preview(preview)
                            .tags(tagRefMap.getOrDefault(note.getId(), List.of()))
                            .createdAt(note.getCreatedAt()).updatedAt(note.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private SmartFolder findOrThrow(Long folderId, Long userId) {
        return smartFolderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private static String joinTagIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static List<Long> parseTagIds(String tagIds) {
        if (tagIds == null || tagIds.isBlank()) return List.of();
        return Arrays.stream(tagIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(Collectors.toList());
    }

    private SmartFolderResponse toResponse(SmartFolder f) {
        return SmartFolderResponse.builder()
                .id(f.getId()).name(f.getName())
                .tagIds(parseTagIds(f.getTagIds()))
                .tagMatchMode(f.getTagMatchMode())
                .createdWithinDays(f.getCreatedWithinDays())
                .keyword(f.getKeyword())
                .createdAt(f.getCreatedAt()).updatedAt(f.getUpdatedAt())
                .build();
    }
}
