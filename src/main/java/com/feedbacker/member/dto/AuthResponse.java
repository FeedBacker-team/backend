package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** 1-3 회원가입 / 2-1 로그인 응답 */
public record AuthResponse(
        @JsonProperty("user_id") UUID userId,
        String email,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("is_profile_completed") boolean profileCompleted
) {
}
