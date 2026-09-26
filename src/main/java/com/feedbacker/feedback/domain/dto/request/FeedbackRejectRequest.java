package com.feedbacker.feedback.domain.dto.request;

import com.feedbacker.feedback.domain.type.RejectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackRejectRequest(

        @NotNull(message = "거부 유형은 필수입니다.")
        RejectType rejectType,

        @NotBlank(message = "거부 상세 사유는 필수입니다.")
        @Size(min = 100, message = "거부 상세 사유는 100자 이상 작성해주셔야 합니다.")
        String rejectDetail
) {
}
