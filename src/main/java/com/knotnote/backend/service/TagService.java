package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.TagCreateRequest;
import com.knotnote.backend.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> getTags(Long userId);
    TagResponse createTag(TagCreateRequest request, Long userId);
    void deleteTag(Long tagId, Long userId);
    void addTagToNote(Long noteId, Long tagId, Long userId);
    void removeTagFromNote(Long noteId, Long tagId, Long userId);
}
