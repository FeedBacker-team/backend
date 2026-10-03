package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;

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
        String thumbnail
) {
    public static FeedbackResponse from(Feedback feedback, FeedbackPost feedbackPost, String thumbnail) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getFeedbackPostId(),
                feedback.getPostTitle(),
                feedback.getStatus(),
                feedback.getRewardAcorn(),
                feedback.getRejectType(),
                null,
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                thumbnail
        );
    }

    // 제출 전에는 Feedback이 없으므로 참여 정보로 응답을 만든다 (id는 null)
    public static FeedbackResponse from(Participation participation, LocalDateTime now, String thumbnail) {
        FeedbackPost feedbackPost = participation.getFeedbackPost();
        return new FeedbackResponse(
                null,
                feedbackPost.getId(),
                feedbackPost.getTitle(),
                toFeedbackStatus(participation, now),
                null,
                null,
                participation.getSubmissionDeadlineAt(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                thumbnail
        );
    }

    private static FeedbackStatus toFeedbackStatus(Participation participation, LocalDateTime now) {
        if (participation.isAbandoned()) {
            return FeedbackStatus.CANCELED;
        }
        // 만료 스케줄러가 돌기 전이라도 제출 기한이 지났으면 만료로 보여준다
        if (participation.isExpired() || participation.isDeadlineReached(now)) {
            return FeedbackStatus.EXPIRED;
        }
        return FeedbackStatus.WRITING;
    }

}
