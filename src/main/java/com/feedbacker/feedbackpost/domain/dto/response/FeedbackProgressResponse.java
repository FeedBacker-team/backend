package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackProgressResponse(
        UUID feedbackId,
        String testerName,
        String testerProfileImageUrl,
        FeedbackStatus status,
        LocalDateTime submitAt,
        LocalDateTime responseDeadlineAt
) {

    public static FeedbackProgressResponse from(Feedback feedback, String testerProfileImageUrl) {
        return new FeedbackProgressResponse(
                feedback.getId(),
                feedback.getTester().getNickname(),
                testerProfileImageUrl,
                feedback.getStatus(),
                feedback.getSubmitAt(),
                feedback.getResponseDeadLineAt()
        );
    }
}
