package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 2-5 비밀번호 재설정 요청 */
public record PasswordResetRequest(
        @NotBlank(message = "올바른 이메일 형식이 아닙니다.")
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "인증번호 6자리를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호 6자리를 입력해 주세요.")
        String code,

        // 회원가입(1-3)과 같은 비밀번호 규칙
        @JsonProperty("new_password")
        @NotBlank(message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
        @Size(max = 64, message = "비밀번호는 64자 이하로 입력해 주세요.")
        String newPassword
) {
}
