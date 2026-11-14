package com.feedbacker.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 2-1 이메일 로그인 요청 */
public record LoginRequest(
        @NotBlank(message = "이메일 형식과 비밀번호를 올바르게 입력해 주세요.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "이메일 형식과 비밀번호를 올바르게 입력해 주세요.")
        String email,

        @NotBlank(message = "이메일 형식과 비밀번호를 올바르게 입력해 주세요.")
        String password
) {
}
