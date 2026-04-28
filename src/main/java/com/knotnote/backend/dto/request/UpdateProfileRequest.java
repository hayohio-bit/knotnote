package com.knotnote.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    /** 변경할 닉네임 (null = 변경 없음) */
    @Size(min = 2, max = 50, message = "닉네임은 2~50자여야 합니다")
    private String nickname;

    /** 새 비밀번호로 변경할 경우 현재 비밀번호 필수 */
    private String currentPassword;

    /**
     * 새 비밀번호 (null = 변경 없음)
     * 영문·숫자·특수문자 조합 8자 이상
     */
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}
