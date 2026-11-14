package com.feedbacker.feedback.domain.dto.request;

import com.feedbacker.feedback.domain.type.ObjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackObjectRequest (
        @NotNull(message = "이의제기 유형은 필수입니다.")
        ObjectType objectType,

        @NotBlank(message = "이의제기 사유는 공백일 수 없습니다.")
        String objectReason
) {
}
