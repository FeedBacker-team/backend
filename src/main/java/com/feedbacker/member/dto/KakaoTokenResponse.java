package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 서버 응답: 인가 코드 → 카카오 액세스 토큰 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(
        @JsonProperty("access_token") String accessToken
) {
}
