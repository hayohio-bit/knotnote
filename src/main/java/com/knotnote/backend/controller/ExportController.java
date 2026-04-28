package com.knotnote.backend.controller;

import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "지식 베이스 내보내기 API")
public class ExportController {

    private final ExportService exportService;

    @GetMapping
    @Operation(
            summary = "지식 베이스 내보내기",
            description = "format=json: 전체 노트·링크·태그를 JSON ZIP으로 내보냅니다. "
                    + "format=markdown: 노트별 Markdown 파일 + INDEX.md를 ZIP으로 내보냅니다."
    )
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "json") String format) {

        byte[] zip = exportService.export(SecurityUtil.currentUserId(), format);

        String filename = "knotnote-export-" + format + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(zip);
    }
}
