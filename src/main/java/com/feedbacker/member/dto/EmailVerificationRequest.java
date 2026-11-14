package com.feedbacker.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 1-1 이메일 인증번호 발송 요청 */
public record EmailVerificationRequest(
        @NotBlank(message = "올바른 이메일 형식이 아닙니다.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "올바른 이메일 형식이 아닙니다.")
        String email
) {
}
