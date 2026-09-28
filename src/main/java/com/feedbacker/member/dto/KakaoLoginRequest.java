package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 3-1 카카오 로그인 요청 */
public record KakaoLoginRequest(
        @JsonProperty("authorization_code")
        @NotBlank(message = "카카오 인가 코드가 누락되었습니다.")
        String authorizationCode
) {
}
