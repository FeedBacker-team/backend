package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 서버 응답: 사용자 정보 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            @JsonProperty("is_email_valid") Boolean emailValid,
            @JsonProperty("is_email_verified") Boolean emailVerified
    ) {
    }

    /** 유효하고 인증된 이메일만 사용 (동의 안 했으면 null) */
    public String verifiedEmailOrNull() {
        if (kakaoAccount == null || kakaoAccount.email() == null) {
            return null;
        }
        boolean valid = Boolean.TRUE.equals(kakaoAccount.emailValid());
        boolean verified = Boolean.TRUE.equals(kakaoAccount.emailVerified());
        return valid && verified ? kakaoAccount.email() : null;
    }
}
