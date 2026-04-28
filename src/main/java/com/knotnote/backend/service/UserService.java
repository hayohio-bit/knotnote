package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.UpdateProfileRequest;
import com.knotnote.backend.dto.response.UserResponse;

public interface UserService {
    UserResponse getMe(Long userId);
    /** 닉네임·비밀번호 변경 (null 필드는 변경 없음) */
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
}
