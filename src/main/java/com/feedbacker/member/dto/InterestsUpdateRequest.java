package com.feedbacker.member.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InterestsUpdateRequest(
        @NotNull(message = "관심 분야 목록은 필수입니다.")
        List<@NotNull(message = "관심 분야 목록은 필수입니다.") String> interests
        ) {
}
