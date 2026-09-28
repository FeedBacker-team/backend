package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 2-2 토큰 재발급 응답 */
public record TokenRefreshResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("is_profile_completed") boolean profileCompleted
) {
}
