package com.knotnote.backend.service;

public interface ExportService {
    /**
     * 사용자의 전체 지식 베이스를 ZIP 바이트 배열로 반환
     *
     * @param userId 요청한 사용자 ID
     * @param format "json" 또는 "markdown"
     * @return ZIP 바이트 배열
     */
    byte[] export(Long userId, String format);
}
