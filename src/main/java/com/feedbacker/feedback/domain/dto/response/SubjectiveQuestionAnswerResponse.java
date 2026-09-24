package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.global.image.ImageResponse;

import java.util.List;

public record SubjectiveQuestionAnswerResponse(
        Integer order,
        String questionText,
        String answerText,
        List<ImageResponse> images
) {
}
