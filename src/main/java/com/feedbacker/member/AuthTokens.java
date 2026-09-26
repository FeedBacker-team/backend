package com.feedbacker.member;

/** 로그인 성공 시 발급되는 토큰 한 쌍 */
public record AuthTokens(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
