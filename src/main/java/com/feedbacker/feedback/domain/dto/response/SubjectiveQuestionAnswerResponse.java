package com.feedbacker.feedback.domain.dto.response;

public record SubjectiveQuestionAnswerResponse(
        Integer order,
        String questionText,
        String answerText,
        String imageUrl
) {
}
