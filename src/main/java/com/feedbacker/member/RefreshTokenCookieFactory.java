package com.feedbacker.member;

import com.feedbacker.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** refreshToken 쿠키 생성/삭제 (HttpOnly; Secure; SameSite=None; Path=/api/auth) */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";

    private final JwtTokenProvider jwtTokenProvider;

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken)
                .maxAge(jwtTokenProvider.getRefreshTokenExpirationSeconds())
                .build();
    }

    public ResponseCookie expire() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH);
    }
}
