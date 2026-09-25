package com.feedbacker.member.dto;

/** 1-2 이메일 인증번호 확인 응답 */
public record EmailVerifyResponse(
        boolean verified,
        String message
) {
}
