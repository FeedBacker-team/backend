package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 1-1 이메일 인증번호 발송 응답 */
public record EmailVerificationResponse(
        String message,
        @JsonProperty("expires_in") long expiresIn
) {
}
