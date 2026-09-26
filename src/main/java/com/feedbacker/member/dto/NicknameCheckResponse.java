package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 4-1 닉네임 중복 확인 응답 */
public record NicknameCheckResponse(
        @JsonProperty("is_available") boolean available,
        String message
) {
}
