package com.feedbacker.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 1-2 이메일 인증번호 확인 요청 */
public record EmailVerifyRequest(
        @NotBlank(message = "올바른 이메일 형식이 아닙니다.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증번호 6자리를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호 6자리를 입력해 주세요.")
        String code
) {
}
