package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubjectiveQuestionAnswerRequest(

        @NotNull(message = "질문 순서는 필수입니다.")
        @Positive(message = "질문 순서는 1 이상이어야 합니다.")
        Long order,

        String text
) {
}
