package com.feedbacker.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "요청 형식이 올바르지 않습니다. 이메일 형식을 확인해주세요.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    private String password;
}