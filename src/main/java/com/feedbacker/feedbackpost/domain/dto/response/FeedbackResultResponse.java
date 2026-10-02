package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.domain.type.FeedbackStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResultResponse(
        UUID feedbackId,
        String testerName,
        LocalDateTime submitAt,
        FeedbackStatus status,
        QuestionAnswerResponse questionAnswer
) {
    public static FeedbackResultResponse from(
            Feedback feedback,
            QuestionAnswerResponse questionAnswer
    ) {
        return new FeedbackResultResponse(
                feedback.getId(),
                feedback.getTester().getNickname(),
                feedback.getSubmitAt(),
                feedback.getStatus(),
                questionAnswer
        );
    }
}
