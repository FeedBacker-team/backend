package com.feedbacker.feedback.domain.dto.response;

import java.util.List;

public record ChoiceQuestionAnswerResponse(
        Integer order,
        String questionText,
        List<String> optionText,
        Integer optionCount,
        Integer selectedOption,
        String imageUrl
) {
}
