package com.knotnote.backend.service;

import com.knotnote.backend.dto.request.LoginRequest;
import com.knotnote.backend.dto.request.SignupRequest;
import com.knotnote.backend.dto.request.TokenRefreshRequest;
import com.knotnote.backend.dto.response.AuthResponse;
import com.knotnote.backend.dto.response.TokenResponse;
import com.knotnote.backend.entity.RefreshToken;
import com.knotnote.backend.entity.User;
import com.knotnote.backend.exception.CustomException;
import com.knotnote.backend.exception.ErrorCode;
import com.knotnote.backend.repository.RefreshTokenRepository;
import com.knotnote.backend.repository.UserRepository;
import com.knotnote.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        rt -> rt.updateToken(refreshToken, refreshExpiresAt),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(user.getId())
                                .token(refreshToken)
                                .expiresAt(refreshExpiresAt)
                                .build())
                );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiration / 1000)
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        // 토큰이 유효하지 않아도 조용히 성공 (幂等性 보장)
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public TokenResponse refresh(TokenRefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        RefreshToken saved = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        Long userId = jwtTokenProvider.getUserId(request.getRefreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        String newAccessToken = jwtTokenProvider.createAccessToken(userId, user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000);

        saved.updateToken(newRefreshToken, refreshExpiresAt);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiration / 1000)
                .build();
    }
}
