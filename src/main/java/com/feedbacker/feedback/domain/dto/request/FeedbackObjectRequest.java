package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackObjectRequest (
        @NotBlank(message = "이의제기 사유는 공백일 수 없습니다.")
        String objectReason
) {
}
