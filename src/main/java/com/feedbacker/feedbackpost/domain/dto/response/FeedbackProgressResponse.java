package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackProgressResponse(
        UUID feedbackId,
        String testerName,
        FeedbackStatus status,
        LocalDateTime submitAt,
        LocalDateTime responseDeadlineAt
) {

    private static final long DEADLINE = 72;

    public static FeedbackProgressResponse from(Feedback feedback) {
        return new FeedbackProgressResponse(
                feedback.getId(),
                feedback.getTesterName(),
                feedback.getStatus(),
                feedback.getSubmitAt(),
                feedback.getSubmitAt().plusHours(DEADLINE)
        );
    }
}
