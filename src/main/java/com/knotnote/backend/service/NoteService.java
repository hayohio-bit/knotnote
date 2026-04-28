package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.CrystallizeRequest;
import com.knotnote.backend.dto.request.NoteCreateRequest;
import com.knotnote.backend.dto.request.NoteLinkRequest;
import com.knotnote.backend.dto.request.NoteUpdateRequest;
import com.knotnote.backend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoteService {
    Page<NoteSummaryResponse> getNotes(Long userId, Pageable pageable);
    NoteDetailResponse getNote(Long noteId, Long userId);
    NoteDetailResponse createNote(NoteCreateRequest request, Long userId);
    NoteDetailResponse updateNote(Long noteId, NoteUpdateRequest request, Long userId);
    void deleteNote(Long noteId, Long userId);
    List<NoteSummaryResponse> getLinkedNotes(Long noteId, Long userId);
    void linkNote(Long noteId, NoteLinkRequest request, Long userId);
    void unlinkNote(Long noteId, Long targetNoteId, Long userId);
    GraphResponse getGraph(Long userId);
    List<RecommendationResponse> getRecommendations(Long noteId, Long userId, int topN);

    /** Crystallize Mode: 링크에 요약 입력하고 확정 */
    CrystallizeResponse crystallizeLink(Long noteId, Long linkId, CrystallizeRequest request, Long userId);

    /** Crystallize Mode: 이 노트의 미확정(점선) 링크 목록 */
    List<PendingLinkResponse> getPendingLinks(Long noteId, Long userId);

    /** Knot Decay: 활력도 낮은 허브 노트 알림 목록 */
    List<DecayAlertResponse> getDecayAlerts(Long userId);

    /** AI 태그 추천 */
    List<TagSuggestionResponse> suggestTags(Long noteId, Long userId, int topN);

    // ── Phase 4: 버전 이력 ────────────────────────────────────────────

    /** 노트의 수정 이력 목록 (최신순) */
    List<NoteVersionResponse> getVersions(Long noteId, Long userId);

    /** 특정 버전으로 노트 내용 복원 */
    NoteDetailResponse restoreVersion(Long noteId, Long versionId, Long userId);

    // ── Phase 5: 고정 ─────────────────────────────────────────────────

    /** 노트 상단 고정 */
    NoteDetailResponse pinNote(Long noteId, Long userId);

    /** 노트 고정 해제 */
    NoteDetailResponse unpinNote(Long noteId, Long userId);

    /** Vitality Score 낮은 노트 목록 (threshold 미만) */
    List<NoteSummaryResponse> getLowVitalityNotes(Long userId, double threshold);

    // ── Phase 5: 일괄 작업 ────────────────────────────────────────────

    /** 여러 노트 일괄 소프트 삭제 */
    void bulkDelete(List<Long> noteIds, Long userId);

    /** 여러 노트에 태그 일괄 부착 */
    void bulkAddTag(List<Long> noteIds, Long tagId, Long userId);
}
