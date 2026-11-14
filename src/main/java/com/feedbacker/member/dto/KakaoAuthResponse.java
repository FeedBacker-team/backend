package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** 3-1 카카오 로그인 / 자동 가입 응답 */
public record KakaoAuthResponse(
        @JsonProperty("user_id") UUID userId,
        String email,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("is_new_user") boolean newUser,
        @JsonProperty("is_profile_completed") boolean profileCompleted
) {
}
