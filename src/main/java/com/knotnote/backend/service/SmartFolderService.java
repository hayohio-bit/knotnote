package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.SmartFolderRequest;
import com.knotnote.backend.dto.response.NoteSummaryResponse;
import com.knotnote.backend.dto.response.SmartFolderResponse;

import java.util.List;

public interface SmartFolderService {
    List<SmartFolderResponse> list(Long userId);
    SmartFolderResponse create(SmartFolderRequest request, Long userId);
    SmartFolderResponse update(Long folderId, SmartFolderRequest request, Long userId);
    void delete(Long folderId, Long userId);
    List<NoteSummaryResponse> getNotes(Long folderId, Long userId);
}
