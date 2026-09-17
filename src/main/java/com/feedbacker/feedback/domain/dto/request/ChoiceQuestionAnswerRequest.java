package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChoiceQuestionAnswerRequest(

        @NotNull
        Long questionId,

        @Min(1) @Max(5)
        Integer selectedOption
) {
}
