package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubjectiveQuestionAnswerRequest(

        @NotNull
        Long questionId,

        @NotBlank
        String text
) {
}
