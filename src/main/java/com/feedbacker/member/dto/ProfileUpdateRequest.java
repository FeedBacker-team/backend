package com.feedbacker.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileUpdateRequest {

    @NotBlank(message = "요청 형식이 올바르지 않습니다. 닉네임 또는 직군 값을 확인해주세요.")
    @Size(min = 2, max = 20, message = "요청 형식이 올바르지 않습니다. 닉네임 또는 직군 값을 확인해주세요.")
    private String nickname;

    @NotBlank(message = "요청 형식이 올바르지 않습니다. 닉네임 또는 직군 값을 확인해주세요.")
    @Pattern(regexp = "^(?i)(developer|designer)$", message = "요청 형식이 올바르지 않습니다. 닉네임 또는 직군 값을 확인해주세요.")
    private String role;
}