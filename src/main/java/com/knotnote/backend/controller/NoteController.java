package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.request.BulkDeleteRequest;
import com.knotnote.backend.dto.request.BulkTagRequest;
import com.knotnote.backend.dto.request.CrystallizeRequest;
import com.knotnote.backend.dto.request.NoteCreateRequest;
import com.knotnote.backend.dto.request.NoteLinkRequest;
import com.knotnote.backend.dto.request.NoteUpdateRequest;
import com.knotnote.backend.dto.response.*;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "메모 API")
public class NoteController {

    private final NoteService noteService;

    // ── 기본 CRUD ──────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "메모 목록 조회")
    public ApiResponse<Page<NoteSummaryResponse>> getNotes(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(noteService.getNotes(SecurityUtil.currentUserId(), pageable));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "메모 단건 조회")
    public ApiResponse<NoteDetailResponse> getNote(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getNote(noteId, SecurityUtil.currentUserId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "메모 생성")
    public ApiResponse<NoteDetailResponse> createNote(
            @Valid @RequestBody NoteCreateRequest request) {
        return ApiResponse.ok(noteService.createNote(request, SecurityUtil.currentUserId()));
    }

    @PatchMapping("/{noteId}")
    @Operation(summary = "메모 수정")
    public ApiResponse<NoteDetailResponse> updateNote(
            @PathVariable Long noteId,
            @RequestBody NoteUpdateRequest request) {
        return ApiResponse.ok(noteService.updateNote(noteId, request, SecurityUtil.currentUserId()));
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "메모 삭제 (소프트)")
    public ApiResponse<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    // ── 링크 관리 ───────────────────────────────────────────────────

    @GetMapping("/{noteId}/links")
    @Operation(summary = "연결된 메모 목록 조회")
    public ApiResponse<List<NoteSummaryResponse>> getLinkedNotes(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getLinkedNotes(noteId, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{noteId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "메모 링크 연결",
            description = "intent 필드로 연결 이유를 메모하면 Knot Strength Score가 높아집니다")
    public ApiResponse<Void> linkNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteLinkRequest request) {
        noteService.linkNote(noteId, request, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{noteId}/links/{targetNoteId}")
    @Operation(summary = "메모 링크 해제")
    public ApiResponse<Void> unlinkNote(
            @PathVariable Long noteId,
            @PathVariable Long targetNoteId) {
        noteService.unlinkNote(noteId, targetNoteId, SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    // ── Crystallize Mode ───────────────────────────────────────────

    @PatchMapping("/{noteId}/links/{linkId}/crystallize")
    @Operation(summary = "링크 Crystallize 확정",
            description = "두 메모의 관계를 한 문장으로 요약하면 점선이 실선으로 바뀌고 Knot Strength가 재계산됩니다")
    public ApiResponse<CrystallizeResponse> crystallizeLink(
            @PathVariable Long noteId,
            @PathVariable Long linkId,
            @Valid @RequestBody CrystallizeRequest request) {
        return ApiResponse.ok(
                noteService.crystallizeLink(noteId, linkId, request, SecurityUtil.currentUserId()));
    }


    @GetMapping("/{noteId}/links/pending")
    @Operation(summary = "미확정 링크 목록 조회 (Crystallize Mode)",
            description = "crystallized=false인 링크 목록 반환. Crystallize 모달에서 사용.")
    public ApiResponse<List<PendingLinkResponse>> getPendingLinks(@PathVariable Long noteId) {
        return ApiResponse.ok(
                noteService.getPendingLinks(noteId, SecurityUtil.currentUserId()));
    }

    // ── Knot Decay ─────────────────────────────────────────────────

    @GetMapping("/decay-alerts")
    @Operation(summary = "Knot Decay 알림 조회",
            description = "Vitality Score가 낮은 허브 메모 목록 (최대 5개). 연결이 많을수록 우선 알림.")
    public ApiResponse<List<DecayAlertResponse>> getDecayAlerts() {
        return ApiResponse.ok(noteService.getDecayAlerts(SecurityUtil.currentUserId()));
    }

    // ── 지식 그래프 ────────────────────────────────────────────────

    @GetMapping("/graph")
    @Operation(summary = "지식 그래프 조회",
            description = "노드에 vitalityScore, 엣지에 strength·crystallized 포함")
    public ApiResponse<GraphResponse> getGraph() {
        return ApiResponse.ok(noteService.getGraph(SecurityUtil.currentUserId()));
    }

    // ── 버전 이력 (Phase 4) ─────────────────────────────────────────

    @GetMapping("/{noteId}/versions")
    @Operation(
            summary = "노트 수정 이력 조회",
            description = "수정이 발생할 때마다 저장된 스냅샷 목록을 최신순으로 반환합니다."
    )
    public ApiResponse<List<NoteVersionResponse>> getVersions(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.getVersions(noteId, SecurityUtil.currentUserId()));
    }

    @PostMapping("/{noteId}/versions/{versionId}/restore")
    @Operation(
            summary = "특정 버전으로 복원",
            description = "선택한 버전의 제목·내용으로 노트를 되돌립니다. 복원 전 현재 상태도 이력에 저장됩니다."
    )
    public ApiResponse<NoteDetailResponse> restoreVersion(
            @PathVariable Long noteId,
            @PathVariable Long versionId) {
        return ApiResponse.ok(
                noteService.restoreVersion(noteId, versionId, SecurityUtil.currentUserId()));
    }

    // ── AI 태그 추천 ────────────────────────────────────────────────

    @GetMapping("/{noteId}/tag-suggestions")
    @Operation(
            summary = "AI 태그 추천",
            description = "임베딩 유사도(없으면 Jaccard 폴백) 기반으로 유사 노트들의 태그를 랭킹합니다. "
                    + "이미 붙어있는 태그는 제외됩니다."
    )
    public ApiResponse<List<TagSuggestionResponse>> suggestTags(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "5") int topN) {
        return ApiResponse.ok(
                noteService.suggestTags(noteId, SecurityUtil.currentUserId(), topN));
    }

    // ── 추천 ────────────────────────────────────────────────────────

    @GetMapping("/{noteId}/recommendations")
    @Operation(summary = "스마트 연결 추천",
            description = "공통 태그(Jaccard) + 공통 이웃(그래프 코사인) 기반 추천")
    public ApiResponse<List<RecommendationResponse>> getRecommendations(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "5") int topN) {
        return ApiResponse.ok(
                noteService.getRecommendations(noteId, SecurityUtil.currentUserId(), topN));
    }

    // ── Phase 5: 고정 ────────────────────────────────────────────────

    @PostMapping("/{noteId}/pin")
    @Operation(summary = "노트 상단 고정", description = "고정된 노트는 목록 최상단에 노출됩니다.")
    public ApiResponse<NoteDetailResponse> pinNote(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.pinNote(noteId, SecurityUtil.currentUserId()));
    }

    @DeleteMapping("/{noteId}/pin")
    @Operation(summary = "노트 고정 해제")
    public ApiResponse<NoteDetailResponse> unpinNote(@PathVariable Long noteId) {
        return ApiResponse.ok(noteService.unpinNote(noteId, SecurityUtil.currentUserId()));
    }

    // ── Phase 5: Vitality 낮은 노트 ──────────────────────────────────

    @GetMapping("/low-vitality")
    @Operation(
            summary = "활력도 낮은 노트 목록",
            description = "vitalityScore 가 threshold 미만인 노트를 낮은 순서로 반환합니다."
    )
    public ApiResponse<List<NoteSummaryResponse>> getLowVitalityNotes(
            @RequestParam(defaultValue = "0.3") double threshold) {
        return ApiResponse.ok(
                noteService.getLowVitalityNotes(SecurityUtil.currentUserId(), threshold));
    }

    // ── Phase 5: 일괄 작업 ───────────────────────────────────────────

    @PostMapping("/bulk/delete")
    @Operation(summary = "노트 일괄 삭제", description = "여러 노트를 한 번에 소프트 삭제합니다. 소유하지 않은 ID는 무시됩니다.")
    public ApiResponse<Void> bulkDelete(@Valid @RequestBody BulkDeleteRequest request) {
        noteService.bulkDelete(request.getNoteIds(), SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/bulk/tag")
    @Operation(summary = "노트 일괄 태그 부착", description = "여러 노트에 같은 태그를 한 번에 부착합니다. 이미 부착된 경우 무시됩니다.")
    public ApiResponse<Void> bulkAddTag(@Valid @RequestBody BulkTagRequest request) {
        noteService.bulkAddTag(request.getNoteIds(), request.getTagId(), SecurityUtil.currentUserId());
        return ApiResponse.ok(null);
    }
}
