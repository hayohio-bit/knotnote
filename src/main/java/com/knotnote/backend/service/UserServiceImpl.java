package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.UpdateProfileRequest;
import com.knotnote.backend.dto.response.UserResponse;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    @Override
    public UserResponse getMe(Long userId) {
        return toResponse(findOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findOrThrow(userId);

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.updateNickname(request.getNickname());
        }

        // 비밀번호 변경
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
            }
            user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return toResponse(user);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
