package com.knotnote.backend.service;

import com.knotnote.backend.dto.response.StatsResponse;
import com.knotnote.backend.dto.response.StatsResponse.NoteSummary;
import com.knotnote.backend.dto.response.StatsResponse.TagStats;
import com.knotnote.backend.dto.response.StatsResponse.VitalityDistribution;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.Tag;
import com.knotnote.backend.repository.NoteLinkRepository;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import com.knotnote.backend.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final int TOP_TAGS_LIMIT = 5;

    private final NoteRepository     noteRepository;
    private final NoteLinkRepository noteLinkRepository;
    private final NoteTagRepository  noteTagRepository;
    private final TagRepository      tagRepository;

    @Override
    public StatsResponse getStats(Long userId) {

        // ── 노트 집계 ──────────────────────────────────────────────
        List<Note> notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        long totalNotes = notes.size();

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentNoteCount = notes.stream()
                .filter(n -> n.getCreatedAt() != null && n.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        // ── 링크 집계 ──────────────────────────────────────────────
        long totalLinks        = noteLinkRepository.countByUserId(userId);
        long crystallizedLinks = noteLinkRepository.countCrystallizedByUserId(userId);
        double crystallizationRate = totalLinks == 0
                ? 0.0
                : Math.round(crystallizedLinks * 1000.0 / totalLinks) / 10.0; // 소수점 1자리 %

        // ── 태그 집계 ──────────────────────────────────────────────
        List<Tag> tags = tagRepository.findByUserId(userId);
        long totalTags = tags.size();

        List<TagStats> topTags = tags.stream()
                .map(tag -> {
                    long cnt = noteTagRepository.countActiveByTagId(tag.getId());
                    return TagStats.builder()
                            .tagId(tag.getId())
                            .tagName(tag.getName())
                            .noteCount(cnt)
                            .build();
                })
                .filter(ts -> ts.getNoteCount() > 0)
                .sorted(Comparator.comparingLong(TagStats::getNoteCount).reversed())
                .limit(TOP_TAGS_LIMIT)
                .collect(Collectors.toList());

        // ── Vitality 분포 ──────────────────────────────────────────
        long high   = notes.stream().filter(n -> n.getVitalityScore() >= 0.7).count();
        long medium = notes.stream().filter(n -> n.getVitalityScore() >= 0.3
                                                  && n.getVitalityScore() < 0.7).count();
        long low    = notes.stream().filter(n -> n.getVitalityScore() < 0.3).count();

        double avgVitality = notes.isEmpty() ? 0.0
                : Math.round(notes.stream()
                        .mapToDouble(Note::getVitalityScore)
                        .average()
                        .orElse(0.0) * 1000.0) / 1000.0;

        VitalityDistribution dist = VitalityDistribution.builder()
                .high(high).medium(medium).low(low)
                .build();

        // ── 가장 연결이 많은 노트 ──────────────────────────────────
        NoteSummary mostConnected = notes.stream()
                .map(n -> {
                    long linkCnt = noteLinkRepository.countByNoteId(n.getId());
                    return NoteSummary.builder()
                            .noteId(n.getId())
                            .title(n.getTitle())
                            .linkCount(linkCnt)
                            .build();
                })
                .filter(ns -> ns.getLinkCount() > 0)
                .max(Comparator.comparingLong(NoteSummary::getLinkCount))
                .orElse(null);

        return StatsResponse.builder()
                .totalNotes(totalNotes)
                .recentNoteCount(recentNoteCount)
                .totalLinks(totalLinks)
                .crystallizedLinks(crystallizedLinks)
                .crystallizationRate(crystallizationRate)
                .totalTags(totalTags)
                .topTags(topTags)
                .vitalityDistribution(dist)
                .avgVitalityScore(avgVitality)
                .mostConnectedNote(mostConnected)
                .build();
    }
}
