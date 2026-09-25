package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubjectiveQuestionAnswerRequest(

        @NotNull(message = "질문 순서는 필수입니다.")
        Long order,

        String text
) {
}
