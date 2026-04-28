package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.GraphInsightsResponse;
import com.knotnote.backend.dto.response.GraphInsightsResponse.*;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteLink;
import com.knotnote.backend.repository.NoteLinkRepository;
import com.knotnote.backend.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraphInsightsServiceImpl implements GraphInsightsService {

    private static final double WEAK_LINK_THRESHOLD = 0.3;
    private static final int    TOP_HUB_LIMIT       = 5;
    private static final int    WEAK_LINK_LIMIT      = 10;

    private final NoteRepository     noteRepository;
    private final NoteLinkRepository noteLinkRepository;

    @Override
    public GraphInsightsResponse getInsights(Long userId) {
        List<Note>     notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<NoteLink> links = noteLinkRepository.findAllByUserId(userId);

        if (notes.isEmpty()) {
            return GraphInsightsResponse.builder()
                    .orphanNotes(List.of())
                    .hubNotes(List.of())
                    .weakLinks(List.of())
                    .clusterCount(0)
                    .connectivityRate(0.0)
                    .crystallizationRate(0.0)
                    .build();
        }

        // ── 인접 맵 구성 ────────────────────────────────────────────
        Map<Long, Set<Long>> adjacency = new HashMap<>();
        Map<Long, Integer>   degree    = new HashMap<>();
        Map<Long, Long>      crystallizedDegree = new HashMap<>();

        for (Note n : notes) {
            adjacency.put(n.getId(), new HashSet<>());
            degree.put(n.getId(), 0);
            crystallizedDegree.put(n.getId(), 0L);
        }

        for (NoteLink link : links) {
            Long from = link.getFromNote().getId();
            Long to   = link.getToNote().getId();
            adjacency.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            adjacency.computeIfAbsent(to,   k -> new HashSet<>()).add(from);
            degree.merge(from, 1, Integer::sum);
            degree.merge(to,   1, Integer::sum);
            if (link.isCrystallized()) {
                crystallizedDegree.merge(from, 1L, Long::sum);
                crystallizedDegree.merge(to,   1L, Long::sum);
            }
        }

        Map<Long, Note> noteById = notes.stream()
                .collect(Collectors.toMap(Note::getId, n -> n));

        // ── 고아 노트 ────────────────────────────────────────────────
        List<OrphanNote> orphans = notes.stream()
                .filter(n -> degree.getOrDefault(n.getId(), 0) == 0)
                .map(n -> OrphanNote.builder()
                        .noteId(n.getId())
                        .title(n.getTitle())
                        .vitalityScore(Math.round(n.getVitalityScore() * 1000.0) / 1000.0)
                        .build())
                .collect(Collectors.toList());

        // ── 허브 노트 (상위 5) ───────────────────────────────────────
        List<HubNote> hubs = notes.stream()
                .filter(n -> degree.getOrDefault(n.getId(), 0) > 0)
                .sorted(Comparator.comparingInt((Note n) ->
                        degree.getOrDefault(n.getId(), 0)).reversed())
                .limit(TOP_HUB_LIMIT)
                .map(n -> HubNote.builder()
                        .noteId(n.getId())
                        .title(n.getTitle())
                        .degree(degree.getOrDefault(n.getId(), 0))
                        .crystallized(crystallizedDegree.getOrDefault(n.getId(), 0L))
                        .vitalityScore(Math.round(n.getVitalityScore() * 1000.0) / 1000.0)
                        .build())
                .collect(Collectors.toList());

        // ── 약한 연결 (strength < 0.3, 상위 10) ─────────────────────
        List<WeakLink> weak = links.stream()
                .filter(l -> l.getStrength() < WEAK_LINK_THRESHOLD)
                .sorted(Comparator.comparingDouble(NoteLink::getStrength))
                .limit(WEAK_LINK_LIMIT)
                .map(l -> WeakLink.builder()
                        .linkId(l.getId())
                        .fromNoteId(l.getFromNote().getId())
                        .fromTitle(l.getFromNote().getTitle())
                        .toNoteId(l.getToNote().getId())
                        .toTitle(l.getToNote().getTitle())
                        .strength(Math.round(l.getStrength() * 1000.0) / 1000.0)
                        .crystallized(l.isCrystallized())
                        .build())
                .collect(Collectors.toList());

        // ── 클러스터 수 (BFS로 연결 컴포넌트 탐색) ──────────────────
        int clusterCount = countConnectedComponents(notes, adjacency);

        // ── 연결률 ───────────────────────────────────────────────────
        long connectedNotes = notes.stream()
                .filter(n -> degree.getOrDefault(n.getId(), 0) > 0).count();
        double connectivityRate = notes.isEmpty() ? 0.0
                : Math.round((double) connectedNotes / notes.size() * 1000.0) / 1000.0;

        // ── Crystallize 완성률 ────────────────────────────────────────
        long totalLinks        = links.size();
        long crystallizedLinks = links.stream().filter(NoteLink::isCrystallized).count();
        double crystallizationRate = totalLinks == 0 ? 0.0
                : Math.round((double) crystallizedLinks / totalLinks * 1000.0) / 1000.0;

        return GraphInsightsResponse.builder()
                .orphanNotes(orphans)
                .hubNotes(hubs)
                .weakLinks(weak)
                .clusterCount(clusterCount)
                .connectivityRate(connectivityRate)
                .crystallizationRate(crystallizationRate)
                .build();
    }

    // ── BFS: 연결된 컴포넌트 수 ──────────────────────────────────────

    private int countConnectedComponents(List<Note> notes, Map<Long, Set<Long>> adjacency) {
        Set<Long> visited = new HashSet<>();
        int count = 0;
        for (Note note : notes) {
            Long id = note.getId();
            if (!visited.contains(id)) {
                bfs(id, adjacency, visited);
                count++;
            }
        }
        return count;
    }

    private void bfs(Long start, Map<Long, Set<Long>> adjacency, Set<Long> visited) {
        Queue<Long> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            for (Long neighbor : adjacency.getOrDefault(cur, Set.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
    }
}
