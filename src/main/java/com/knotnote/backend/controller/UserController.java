package com.knotnote.backend.controller;

import com.knotnote.backend.common.ApiResponse;
import com.knotnote.backend.dto.request.UpdateProfileRequest;
import com.knotnote.backend.dto.response.UserResponse;
import com.knotnote.backend.security.SecurityUtil;
import com.knotnote.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "사용자 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회")
    public ApiResponse<UserResponse> getMe() {
        return ApiResponse.ok(userService.getMe(SecurityUtil.currentUserId()));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "프로필 수정",
            description = "닉네임·비밀번호 중 변경할 항목만 포함하면 됩니다. "
                    + "비밀번호 변경 시 currentPassword 필수."
    )
    public ApiResponse<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(SecurityUtil.currentUserId(), request));
    }
}
