package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        String title,
        FeedbackStatus status,
        Integer rewardAcorn,
        RejectType rejectType,
        LocalDateTime submitAt,
        LocalDateTime processedAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getPostTitle(),
                feedback.getStatus(),
                feedback.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getSubmitAt(),
                feedback.getProcessedAt()
        );
    }

    public static List<FeedbackResponse> fromAll(List<Feedback> feedbacks) {
        return feedbacks.stream()
                .map(FeedbackResponse::from)
                .toList();
    }

}
