package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID feedbackPostId,
        String title,
        FeedbackStatus status,
        Integer rewardAcorn,
        RejectType rejectType,
        LocalDateTime submissionDeadlineAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime completeAt,
        String thumbnail
) {
    public static FeedbackResponse from(
            Feedback feedback,
            FeedbackPost feedbackPost,
            String thumbnail,
            LocalDateTime now
    ) {
        return new FeedbackResponse(
                feedback.getId(),
                feedbackPost.getId(),
                feedbackPost.getTitle(),
                toDisplayStatus(feedback, now),
                feedback.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getSubmitAt() == null ? feedback.getExpireAt() : null,
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedbackPost.getCompleteAt(),
                thumbnail
        );
    }

    // 만료 스케줄러가 돌기 전이라도 제출 기한이 지났으면 만료로 보여준다
    private static FeedbackStatus toDisplayStatus(Feedback feedback, LocalDateTime now) {
        if (feedback.getStatus() == FeedbackStatus.WRITING && !now.isBefore(feedback.getExpireAt())) {
            return FeedbackStatus.EXPIRED;
        }
        return feedback.getStatus();
    }

}
