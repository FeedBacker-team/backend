package com.feedbacker.member;

import com.feedbacker.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Access/Refresh Token 발급 및 Refresh Token 저장 (이메일·카카오 로그인 공용) */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthTokens issue(UUID memberId) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId);
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());
        refreshTokenRepository.save(RefreshToken.of(memberId, refreshToken, expiresAt));

        return new AuthTokens(accessToken, refreshToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }
}
