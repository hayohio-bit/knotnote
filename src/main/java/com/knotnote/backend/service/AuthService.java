package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.LoginRequest;
import com.knotnote.backend.dto.request.SignupRequest;
import com.knotnote.backend.dto.request.TokenRefreshRequest;
import com.knotnote.backend.dto.response.AuthResponse;
import com.knotnote.backend.dto.response.TokenResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(TokenRefreshRequest request);
    /** RefreshToken을 DB에서 삭제하여 해당 토큰으로의 재발급을 차단 */
    void logout(String refreshToken);
}
