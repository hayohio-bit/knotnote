package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.CrystallizeRequest;
import com.knotnote.backend.dto.request.NoteCreateRequest;
import com.knotnote.backend.dto.request.NoteLinkRequest;
import com.knotnote.backend.dto.request.NoteUpdateRequest;
import com.knotnote.backend.dto.response.*;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteLink;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.entity.NoteVersion;
import com.knotnote.backend.entity.Tag;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.NoteLinkRepository;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import com.knotnote.backend.entity.ActivityLog.ActivityType;
import com.knotnote.backend.repository.NoteVersionRepository;
import com.knotnote.backend.repository.TagRepository;
import com.knotnote.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteServiceImpl implements NoteService {

    private static final int PREVIEW_LENGTH = 500;

    private final NoteRepository        noteRepository;
    private final NoteLinkRepository    noteLinkRepository;
    private final NoteTagRepository     noteTagRepository;
    private final NoteVersionRepository noteVersionRepository;
    private final TagRepository         tagRepository;
    private final UserRepository        userRepository;
    private final KnotVitalityService   knotVitalityService;
    private final EmbeddingService      embeddingService;
    private final ActivityService       activityService;

    // ── 기본 CRUD ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<NoteSummaryResponse> getNotes(Long userId, Pageable pageable) {
        Page<Note> page = noteRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
        List<Long> noteIds = page.getContent().stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(noteIds);
        return page.map(note -> toSummaryWithTagMap(note, tagMap));
    }

    @Override
    @Transactional(readOnly = true)
    public NoteDetailResponse getNote(Long noteId, Long userId) {
        return toDetail(findNoteOrThrow(noteId, userId));
    }

    @Override
    public NoteDetailResponse createNote(NoteCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Note note = Note.builder()
                .user(user).title(request.getTitle()).content(request.getContent()).build();
        Note saved = noteRepository.save(note);
        embeddingService.indexNoteAsync(saved);   // Phase 3: 비동기 임베딩
        activityService.record(user, ActivityType.NOTE_CREATED, saved);
        return toDetail(saved);
    }

    @Override
    public NoteDetailResponse updateNote(Long noteId, NoteUpdateRequest request, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);

        // Phase 4: 수정 전 상태를 버전으로 스냅샷
        int nextVersion = noteVersionRepository.findMaxVersionNumber(noteId) + 1;
        noteVersionRepository.save(NoteVersion.builder()
                .note(note)
                .title(note.getTitle())
                .content(note.getContent())
                .versionNumber(nextVersion)
                .build());

        note.update(
            request.getTitle()   != null ? request.getTitle()   : note.getTitle(),
            request.getContent() != null ? request.getContent() : note.getContent()
        );
        embeddingService.indexNoteAsync(note);    // Phase 3: 내용 변경 시 임베딩 재인덱싱
        activityService.record(note.getUser(), ActivityType.NOTE_UPDATED, note);
        return toDetail(note);
    }

    @Override
    public void deleteNote(Long noteId, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);
        note.delete();
        activityService.record(note.getUser(), ActivityType.NOTE_DELETED, note);
    }

    // ── 링크 관리 ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getLinkedNotes(Long noteId, Long userId) {
        findNoteOrThrow(noteId, userId);
        List<Note> linked = noteLinkRepository.findAllByNoteId(noteId).stream()
                .map(l -> l.getFromNote().getId().equals(noteId) ? l.getToNote() : l.getFromNote())
                .collect(Collectors.toList());
        List<Long> ids = linked.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(ids);
        return linked.stream().map(n -> toSummaryWithTagMap(n, tagMap)).collect(Collectors.toList());
    }

    @Override
    public void linkNote(Long noteId, NoteLinkRequest request, Long userId) {
        Note from = findNoteOrThrow(noteId, userId);
        Note to   = findNoteOrThrow(request.getTargetNoteId(), userId);
        if (noteId.equals(request.getTargetNoteId())) throw new CustomException(ErrorCode.INVALID_INPUT);
        if (noteLinkRepository.existsByFromNoteIdAndToNoteId(noteId, request.getTargetNoteId())
                || noteLinkRepository.existsByFromNoteIdAndToNoteId(request.getTargetNoteId(), noteId))
            throw new CustomException(ErrorCode.DUPLICATE);

        NoteLink link = NoteLink.builder()
                .fromNote(from).toNote(to)
                .intent(request.getIntent())
                .build();
        NoteLink saved = noteLinkRepository.save(link);

        // 초기 Strength 계산
        double strength = knotVitalityService.calculateStrength(saved);
        saved.updateStrength(strength);
        activityService.record(from.getUser(), ActivityType.LINK_CREATED, from,
                "→ " + to.getTitle());
    }

    @Override
    public void unlinkNote(Long noteId, Long targetNoteId, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);
        NoteLink link = noteLinkRepository.findByFromNoteIdAndToNoteId(noteId, targetNoteId)
                .or(() -> noteLinkRepository.findByFromNoteIdAndToNoteId(targetNoteId, noteId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        String otherTitle = link.getFromNote().getId().equals(noteId)
                ? link.getToNote().getTitle() : link.getFromNote().getTitle();
        noteLinkRepository.delete(link);
        activityService.record(note.getUser(), ActivityType.LINK_DELETED, note,
                "↗ " + otherTitle);
    }

    // ── Crystallize Mode ───────────────────────────────────────────

    @Override
    public CrystallizeResponse crystallizeLink(Long noteId, Long linkId,
                                                CrystallizeRequest request, Long userId) {
        findNoteOrThrow(noteId, userId);  // 노트 소유권 검증

        NoteLink link = noteLinkRepository.findById(linkId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 해당 링크가 이 노트에 속하는지 확인
        boolean belongsToNote = link.getFromNote().getId().equals(noteId)
                || link.getToNote().getId().equals(noteId);
        if (!belongsToNote) throw new CustomException(ErrorCode.FORBIDDEN);

        // 링크 소유자 검증
        if (!link.getFromNote().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        link.crystallize(request.getSummary());

        // Strength 재계산 (crystallized = true → intentScore 변동 가능)
        double newStrength = knotVitalityService.calculateStrength(link);
        link.updateStrength(newStrength);

        noteLinkRepository.save(link);
        activityService.record(link.getFromNote().getUser(), ActivityType.LINK_CRYSTALLIZED,
                link.getFromNote(), "↔ " + link.getToNote().getTitle());

        return CrystallizeResponse.builder()
                .linkId(linkId)
                .newStrength(Math.round(newStrength * 1000.0) / 1000.0)
                .crystallizedAt(link.getCrystallizedAt())
                .crystallizeSummary(link.getCrystallizeSummary())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<PendingLinkResponse> getPendingLinks(Long noteId, Long userId) {
        findNoteOrThrow(noteId, userId);
        return noteLinkRepository.findAllByNoteId(noteId).stream()
                .filter(l -> !l.isCrystallized())
                .map(l -> PendingLinkResponse.builder()
                        .linkId(l.getId())
                        .fromNoteId(l.getFromNote().getId())
                        .fromTitle(l.getFromNote().getTitle())
                        .toNoteId(l.getToNote().getId())
                        .toTitle(l.getToNote().getTitle())
                        .strength(Math.round(l.getStrength() * 1000.0) / 1000.0)
                        .build())
                .collect(Collectors.toList());
    }

    // ── Knot Decay Alerts ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DecayAlertResponse> getDecayAlerts(Long userId) {
        return knotVitalityService.generateDecayAlerts(userId);
    }

    // ── 지식 그래프 ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public GraphResponse getGraph(Long userId) {
        List<Note> notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagNameMap = buildTagNameMap(noteIds);

        List<NoteLink> links = noteLinkRepository.findAllByUserId(userId);

        Map<Long, Integer> degreeMap = new HashMap<>();
        for (NoteLink link : links) {
            degreeMap.merge(link.getFromNote().getId(), 1, Integer::sum);
            degreeMap.merge(link.getToNote().getId(), 1, Integer::sum);
        }

        List<GraphResponse.NodeDto> nodes = notes.stream()
                .map(n -> GraphResponse.NodeDto.builder()
                        .id(n.getId()).title(n.getTitle())
                        .tags(tagNameMap.getOrDefault(n.getId(), List.of()))
                        .degree(degreeMap.getOrDefault(n.getId(), 0))
                        .vitalityScore(Math.round(n.getVitalityScore() * 1000.0) / 1000.0)
                        .build())
                .collect(Collectors.toList());

        List<GraphResponse.EdgeDto> edges = links.stream()
                .map(l -> GraphResponse.EdgeDto.builder()
                        .source(l.getFromNote().getId())
                        .target(l.getToNote().getId())
                        .strength(Math.round(l.getStrength() * 1000.0) / 1000.0)
                        .crystallized(l.isCrystallized())
                        .crystallizeSummary(l.getCrystallizeSummary())
                        .linkId(l.getId())
                        .build())
                .collect(Collectors.toList());

        return GraphResponse.builder().nodes(nodes).edges(edges).build();
    }

    // ── 추천 ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long noteId, Long userId, int topN) {
        findNoteOrThrow(noteId, userId);

        List<Note> allNotes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Long> allIds = allNotes.stream().map(Note::getId).collect(Collectors.toList());

        List<NoteTag> allNoteTags = noteTagRepository.findByNoteIdIn(allIds);
        Map<Long, Set<Long>> tagIdMap = allNoteTags.stream().collect(
                Collectors.groupingBy(nt -> nt.getNote().getId(),
                        Collectors.mapping(nt -> nt.getTag().getId(), Collectors.toSet())));
        Map<Long, List<NoteDetailResponse.TagRef>> tagRefMap = allNoteTags.stream().collect(
                Collectors.groupingBy(nt -> nt.getNote().getId(),
                        Collectors.mapping(nt -> NoteDetailResponse.TagRef.builder()
                                .id(nt.getTag().getId()).name(nt.getTag().getName()).build(),
                                Collectors.toList())));

        List<NoteLink> allLinks = noteLinkRepository.findAllByUserId(userId);
        Map<Long, Set<Long>> neighborMap = new HashMap<>();
        for (NoteLink link : allLinks) {
            neighborMap.computeIfAbsent(link.getFromNote().getId(), k -> new HashSet<>())
                    .add(link.getToNote().getId());
            neighborMap.computeIfAbsent(link.getToNote().getId(), k -> new HashSet<>())
                    .add(link.getFromNote().getId());
        }

        Set<Long> excluded = new HashSet<>(neighborMap.getOrDefault(noteId, Set.of()));
        excluded.add(noteId);

        Set<Long> tagsX      = tagIdMap.getOrDefault(noteId, Set.of());
        Set<Long> neighborsX = neighborMap.getOrDefault(noteId, Set.of());
        int degX = neighborsX.size();

        return allNotes.stream()
                .filter(n -> !excluded.contains(n.getId()))
                .map(n -> {
                    Set<Long> tagsY      = tagIdMap.getOrDefault(n.getId(), Set.of());
                    Set<Long> neighborsY = neighborMap.getOrDefault(n.getId(), Set.of());
                    int degY = neighborsY.size();

                    double jTag   = jaccardSimilarity(tagsX, tagsY);
                    long common   = neighborsX.stream().filter(neighborsY::contains).count();
                    double nScore = (degX == 0 || degY == 0) ? 0.0
                            : (double) common / Math.sqrt((double) degX * degY);
                    double score  = Math.round((0.55 * jTag + 0.45 * nScore) * 1000.0) / 1000.0;

                    String preview = n.getContent() != null && n.getContent().length() > PREVIEW_LENGTH
                            ? n.getContent().substring(0, PREVIEW_LENGTH) : n.getContent();

                    return RecommendationResponse.builder()
                            .id(n.getId()).title(n.getTitle()).preview(preview)
                            .tags(tagRefMap.getOrDefault(n.getId(), List.of()))
                            .score(score)
                            .scoreReason(buildReason(tagsX, tagsY, (int) common))
                            .build();
                })
                .filter(r -> r.getScore() > 0.0)
                .sorted(Comparator.comparingDouble(RecommendationResponse::getScore).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ── 버전 이력 ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NoteVersionResponse> getVersions(Long noteId, Long userId) {
        findNoteOrThrow(noteId, userId);  // 소유권 검증
        return noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId).stream()
                .map(v -> NoteVersionResponse.builder()
                        .versionId(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .title(v.getTitle())
                        .content(v.getContent())
                        .savedAt(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public NoteDetailResponse restoreVersion(Long noteId, Long versionId, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);

        NoteVersion version = noteVersionRepository.findByIdAndNoteId(versionId, noteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 복원 전 현재 상태도 이력에 저장
        int nextVersion = noteVersionRepository.findMaxVersionNumber(noteId) + 1;
        noteVersionRepository.save(NoteVersion.builder()
                .note(note)
                .title(note.getTitle())
                .content(note.getContent())
                .versionNumber(nextVersion)
                .build());

        // 선택한 버전으로 덮어쓰기
        note.update(version.getTitle(), version.getContent());
        embeddingService.indexNoteAsync(note);  // 복원된 내용으로 임베딩 재인덱싱
        activityService.record(note.getUser(), ActivityType.NOTE_RESTORED, note,
                "v" + version.getVersionNumber());

        return toDetail(note);
    }

    // ── AI 태그 추천 ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TagSuggestionResponse> suggestTags(Long noteId, Long userId, int topN) {
        Note target = findNoteOrThrow(noteId, userId);

        // 이미 붙어 있는 태그 ID 수집
        Set<Long> alreadyTagged = noteTagRepository.findByNoteId(noteId).stream()
                .map(nt -> nt.getTag().getId())
                .collect(Collectors.toSet());

        // 사용자의 다른 노트 목록
        List<Note> candidates = noteRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                .filter(n -> !n.getId().equals(noteId))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return List.of();

        // 임베딩이 있으면 의미 유사도 기반, 없으면 Jaccard 태그 유사도로 폴백
        Optional<double[]> targetVec = embeddingService.getEmbedding(noteId);

        // 후보 노트들의 태그 맵 (N+1 방지 배치 조회)
        List<Long> candidateIds = candidates.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<NoteTag>> noteTagsMap = noteTagRepository.findByNoteIdIn(candidateIds).stream()
                .collect(Collectors.groupingBy(nt -> nt.getNote().getId()));

        // 현재 노트 태그 (Jaccard 폴백용)
        Set<Long> targetTagIds = noteTagRepository.findByNoteId(noteId).stream()
                .map(nt -> nt.getTag().getId())
                .collect(Collectors.toSet());

        // 태그별 (누적 유사도, 등장 횟수) 집계
        // Map<tagId, [sumSimilarity, count, tag]>
        Map<Long, double[]> tagAccum = new HashMap<>();
        Map<Long, Tag>      tagById  = new HashMap<>();

        for (Note candidate : candidates) {
            double sim;
            if (targetVec.isPresent()) {
                Optional<double[]> candVec = embeddingService.getEmbedding(candidate.getId());
                sim = candVec.map(v -> cosineSimilarity(targetVec.get(), v)).orElse(0.0);
            } else {
                // Jaccard 폴백
                Set<Long> candTagIds = noteTagsMap.getOrDefault(candidate.getId(), List.of())
                        .stream().map(nt -> nt.getTag().getId()).collect(Collectors.toSet());
                sim = jaccardSimilarity(targetTagIds, candTagIds);
            }

            if (sim <= 0.0) continue;

            List<NoteTag> noteTags = noteTagsMap.getOrDefault(candidate.getId(), List.of());
            for (NoteTag nt : noteTags) {
                Long tagId = nt.getTag().getId();
                if (alreadyTagged.contains(tagId)) continue;
                tagById.put(tagId, nt.getTag());
                tagAccum.computeIfAbsent(tagId, k -> new double[]{0.0, 0.0});
                tagAccum.get(tagId)[0] += sim;   // 누적 유사도
                tagAccum.get(tagId)[1] += 1.0;   // 등장 횟수
            }
        }

        if (tagAccum.isEmpty()) return List.of();

        // score = count * avg_similarity
        double maxScore = tagAccum.values().stream()
                .mapToDouble(arr -> arr[1] * (arr[0] / arr[1]))
                .max().orElse(1.0);

        return tagAccum.entrySet().stream()
                .map(e -> {
                    double[] arr   = e.getValue();
                    double avgSim  = arr[0] / arr[1];
                    double rawScore = arr[1] * avgSim;
                    double confidence = Math.round((rawScore / maxScore) * 1000.0) / 1000.0;
                    Tag tag = tagById.get(e.getKey());
                    return TagSuggestionResponse.builder()
                            .tagId(tag.getId())
                            .tagName(tag.getName())
                            .confidence(confidence)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TagSuggestionResponse::getConfidence).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    // ── Phase 5: 고정 ────────────────────────────────────────────────

    @Override
    public NoteDetailResponse pinNote(Long noteId, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);
        note.pin();
        activityService.record(note.getUser(), ActivityType.NOTE_PINNED, note);
        return toDetail(note);
    }

    @Override
    public NoteDetailResponse unpinNote(Long noteId, Long userId) {
        Note note = findNoteOrThrow(noteId, userId);
        note.unpin();
        activityService.record(note.getUser(), ActivityType.NOTE_UNPINNED, note);
        return toDetail(note);
    }

    // ── Phase 5: Vitality 낮은 노트 ──────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<NoteSummaryResponse> getLowVitalityNotes(Long userId, double threshold) {
        // DB 쿼리에서 threshold 미만만 가져와 메모리 낭비 방지
        List<Note> notes = noteRepository.findLowVitality(userId, threshold);
        if (notes.isEmpty()) return List.of();

        List<Long> ids = notes.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(ids);

        return notes.stream()
                .map(n -> toSummaryWithTagMap(n, tagMap))
                .collect(Collectors.toList());
    }

    // ── Phase 5: 일괄 작업 ───────────────────────────────────────────

    @Override
    public void bulkDelete(List<Long> noteIds, Long userId) {
        if (noteIds == null || noteIds.isEmpty()) return;
        List<Note> notes = noteIds.stream()
                .map(id -> noteRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        notes.forEach(Note::delete);
        noteRepository.saveAll(notes);
    }

    @Override
    public void bulkAddTag(List<Long> noteIds, Long tagId, Long userId) {
        if (noteIds == null || noteIds.isEmpty()) return;

        // 태그 소유권 검증
        com.knotnote.backend.entity.Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        for (Long noteId : noteIds) {
            noteRepository.findByIdAndUserIdAndIsDeletedFalse(noteId, userId).ifPresent(note -> {
                if (!noteTagRepository.existsByNoteIdAndTagId(noteId, tagId)) {
                    noteTagRepository.save(
                            com.knotnote.backend.entity.NoteTag.builder()
                                    .note(note).tag(tag).build());
                }
            });
        }
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length || a.length == 0) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────

    private Note findNoteOrThrow(Long noteId, Long userId) {
        return noteRepository.findByIdAndUserIdAndIsDeletedFalse(noteId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private Map<Long, List<NoteDetailResponse.TagRef>> buildTagMap(List<Long> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        return noteTagRepository.findByNoteIdIn(noteIds).stream()
                .collect(Collectors.groupingBy(nt -> nt.getNote().getId(),
                        Collectors.mapping(nt -> NoteDetailResponse.TagRef.builder()
                                .id(nt.getTag().getId()).name(nt.getTag().getName()).build(),
                                Collectors.toList())));
    }

    private Map<Long, List<String>> buildTagNameMap(List<Long> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        return noteTagRepository.findByNoteIdIn(noteIds).stream()
                .collect(Collectors.groupingBy(nt -> nt.getNote().getId(),
                        Collectors.mapping(nt -> nt.getTag().getName(), Collectors.toList())));
    }

    private NoteSummaryResponse toSummaryWithTagMap(Note note,
                                                    Map<Long, List<NoteDetailResponse.TagRef>> tagMap) {
        String preview = note.getContent() != null && note.getContent().length() > PREVIEW_LENGTH
                ? note.getContent().substring(0, PREVIEW_LENGTH) : note.getContent();
        return NoteSummaryResponse.builder()
                .id(note.getId()).title(note.getTitle()).preview(preview)
                .tags(tagMap.getOrDefault(note.getId(), List.of()))
                .createdAt(note.getCreatedAt()).updatedAt(note.getUpdatedAt())
                .isPinned(note.isPinned())
                .build();
    }

    private NoteDetailResponse toDetail(Note note) {
        List<NoteDetailResponse.TagRef> tags = buildTagMap(List.of(note.getId()))
                .getOrDefault(note.getId(), List.of());

        List<NoteLink> links = noteLinkRepository.findAllByNoteId(note.getId());
        List<Note> linkedList = links.stream()
                .map(l -> l.getFromNote().getId().equals(note.getId())
                        ? l.getToNote() : l.getFromNote())
                .collect(Collectors.toList());

        List<Long> linkedIds = linkedList.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<NoteDetailResponse.TagRef>> tagMap = buildTagMap(linkedIds);

        List<NoteSummaryResponse> linkedNotes = linkedList.stream()
                .map(n -> toSummaryWithTagMap(n, tagMap)).collect(Collectors.toList());

        return NoteDetailResponse.builder()
                .id(note.getId()).title(note.getTitle()).content(note.getContent())
                .tags(tags).linkedNotes(linkedNotes)
                .createdAt(note.getCreatedAt()).updatedAt(note.getUpdatedAt())
                .isPinned(note.isPinned())
                .build();
    }

    private static <T> double jaccardSimilarity(Set<T> a, Set<T> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        long intersection = a.stream().filter(b::contains).count();
        long union = (long) a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private static String buildReason(Set<Long> tagsX, Set<Long> tagsY, int commonNeighbors) {
        long commonTags = tagsX.stream().filter(tagsY::contains).count();
        List<String> parts = new ArrayList<>();
        if (commonTags > 0)       parts.add("공통 태그 " + commonTags + "개");
        if (commonNeighbors > 0)  parts.add("공통 이웃 " + commonNeighbors + "개");
        return String.join(" · ", parts);
    }
}
