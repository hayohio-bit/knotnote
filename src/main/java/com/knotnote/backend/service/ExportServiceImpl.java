package com.knotnote.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.knotnote.backend.entity.Note;
import com.knotnote.backend.entity.NoteLink;
import com.knotnote.backend.entity.NoteTag;
import com.knotnote.backend.entity.Tag;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.NoteLinkRepository;
import com.knotnote.backend.repository.NoteRepository;
import com.knotnote.backend.repository.NoteTagRepository;
import com.knotnote.backend.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportServiceImpl implements ExportService {

    private final NoteRepository     noteRepository;
    private final NoteLinkRepository noteLinkRepository;
    private final NoteTagRepository  noteTagRepository;
    private final TagRepository      tagRepository;

    @Override
    public byte[] export(Long userId, String format) {
        try {
            return switch (format.toLowerCase()) {
                case "json"     -> exportAsJson(userId);
                case "markdown" -> exportAsMarkdown(userId);
                default         -> throw new CustomException(ErrorCode.INVALID_INPUT);
            };
        } catch (CustomException e) {
            throw e;
        } catch (IOException e) {
            log.error("Export 실패 userId={}, format={}", userId, format, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // ── JSON 내보내기 ───────────────────────────────────────────────

    private byte[] exportAsJson(Long userId) throws IOException {
        List<Note>     notes = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<NoteLink> links = noteLinkRepository.findAllByUserId(userId);
        List<Tag>      tags  = tagRepository.findByUserId(userId);
        List<Long>     noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = buildTagNameMap(noteIds);

        List<Map<String, Object>> noteDtos = notes.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",           n.getId());
            m.put("title",        n.getTitle());
            m.put("content",      n.getContent());
            m.put("tags",         tagMap.getOrDefault(n.getId(), List.of()));
            m.put("vitalityScore", n.getVitalityScore());
            m.put("createdAt",    n.getCreatedAt());
            m.put("updatedAt",    n.getUpdatedAt());
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> linkDtos = links.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fromNoteId",        l.getFromNote().getId());
            m.put("toNoteId",          l.getToNote().getId());
            m.put("intent",            l.getIntent());
            m.put("strength",          l.getStrength());
            m.put("crystallized",      l.isCrystallized());
            m.put("crystallizeSummary",l.getCrystallizeSummary());
            return m;
        }).collect(Collectors.toList());

        List<Map<String, Object>> tagDtos = tags.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",   t.getId());
            m.put("name", t.getName());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exportedAt", LocalDateTime.now().toString());
        payload.put("noteCount",  noteDtos.size());
        payload.put("linkCount",  linkDtos.size());
        payload.put("tagCount",   tagDtos.size());
        payload.put("notes",      noteDtos);
        payload.put("links",      linkDtos);
        payload.put("tags",       tagDtos);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);

        byte[] json = mapper.writeValueAsBytes(payload);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("knotnote-export.json"));
            zip.write(json);
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    // ── Markdown 내보내기 ───────────────────────────────────────────

    private byte[] exportAsMarkdown(Long userId) throws IOException {
        List<Note>  notes   = noteRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Long>  noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = buildTagNameMap(noteIds);

        List<NoteLink> links = noteLinkRepository.findAllByUserId(userId);
        // noteId → 연결된 노트 제목 목록
        Map<Long, List<String>> linkedTitles = new HashMap<>();
        for (NoteLink l : links) {
            linkedTitles.computeIfAbsent(l.getFromNote().getId(), k -> new ArrayList<>())
                    .add(l.getToNote().getTitle());
            linkedTitles.computeIfAbsent(l.getToNote().getId(), k -> new ArrayList<>())
                    .add(l.getFromNote().getTitle());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            // 노트별 개별 파일
            for (Note note : notes) {
                String filename = sanitizeFilename(note.getTitle()) + ".md";
                zip.putNextEntry(new ZipEntry("notes/" + filename));
                zip.write(buildMarkdown(note, tagMap, linkedTitles).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            // 인덱스 파일
            zip.putNextEntry(new ZipEntry("INDEX.md"));
            zip.write(buildIndex(notes).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return baos.toByteArray();
    }

    private String buildMarkdown(Note note,
                                 Map<Long, List<String>> tagMap,
                                 Map<Long, List<String>> linkedTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(note.getTitle()).append("\n\n");

        List<String> tags = tagMap.getOrDefault(note.getId(), List.of());
        if (!tags.isEmpty()) {
            sb.append("**Tags:** ").append(String.join(", ", tags)).append("\n\n");
        }

        sb.append("**Vitality Score:** ")
          .append(String.format("%.3f", note.getVitalityScore())).append("\n");
        sb.append("**Created:** ").append(note.getCreatedAt()).append("\n");
        sb.append("**Updated:** ").append(note.getUpdatedAt()).append("\n\n");
        sb.append("---\n\n");

        if (note.getContent() != null && !note.getContent().isBlank()) {
            sb.append(note.getContent()).append("\n\n");
        }

        List<String> linked = linkedTitles.getOrDefault(note.getId(), List.of());
        if (!linked.isEmpty()) {
            sb.append("---\n\n**Linked Notes:**\n\n");
            linked.forEach(t -> sb.append("- ").append(t).append("\n"));
        }
        return sb.toString();
    }

    private String buildIndex(List<Note> notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("# KnotNote Knowledge Base Index\n\n");
        sb.append("Total notes: ").append(notes.size()).append("\n\n");
        notes.stream()
                .sorted(Comparator.comparing(Note::getTitle))
                .forEach(n -> sb.append("- [").append(n.getTitle()).append("](notes/")
                        .append(sanitizeFilename(n.getTitle())).append(".md)\n"));
        return sb.toString();
    }

    // ── 헬퍼 ───────────────────────────────────────────────────────

    private Map<Long, List<String>> buildTagNameMap(List<Long> noteIds) {
        if (noteIds.isEmpty()) return Map.of();
        return noteTagRepository.findByNoteIdIn(noteIds).stream()
                .collect(Collectors.groupingBy(nt -> nt.getNote().getId(),
                        Collectors.mapping(nt -> nt.getTag().getName(), Collectors.toList())));
    }

    /** 파일명에 사용 불가능한 문자를 '_'로 치환 */
    private String sanitizeFilename(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
