package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ChoiceQuestionAnswerRequest(

        @NotNull(message = "질문 순서는 필수입니다.")
        @Positive(message = "질문 순서는 1 이상이어야 합니다.")
        Long order,

        List<@NotNull @Min(1) Integer> selectedOption
) {
    public ChoiceQuestionAnswerRequest {
        selectedOption = selectedOption == null ? List.of() : selectedOption;
    }
}
