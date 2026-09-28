package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 4-2 기본 프로필 설정 요청.
 * role, interests는 문자열로 받아 서비스에서 검증 (명세의 에러 메시지를 그대로 주기 위함)
 */
public record ProfileUpdateRequest(
        @JsonProperty("profile_image_url")
        @Size(max = 512, message = "프로필 이미지 URL이 너무 깁니다.")
        String profileImageUrl,

        @NotBlank(message = "닉네임은 10자 이내로 입력해 주세요.")
        @Pattern(regexp = "^\\S{1,10}$", message = "닉네임은 10자 이내로 입력해 주세요.")
        String nickname,

        @NotBlank(message = "직군을 올바르게 선택해 주세요.")
        String role,

        @JsonProperty("intro_link")
        @Size(max = 512, message = "소개 링크가 너무 깁니다.")
        @Pattern(regexp = "^https?://\\S+$", message = "소개 링크는 http:// 또는 https://로 시작하는 URL이어야 합니다.")
        String introLink,

        List<String> interests
) {
}
