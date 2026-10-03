package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackDetailResponse(
        String feedbackPostTitle,
        UUID feedbackPostId,
        String testerName,
        String testerProfileImageUrl,
        FeedbackStatus feedbackStatus,
        TargetType targetType,
        int rewardAcorn,
        RejectType rejectType,
        String rejectDetail,
        ImageResponse thumbnail,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime participateAt,
        LocalDateTime submitAt,
        LocalDateTime responseDeadlineAt,
        QuestionAnswerResponse questionAnswerResponses
) {

    public static FeedbackDetailResponse from(
            FeedbackPost feedbackPost,
            Feedback feedback,
            String testerProfileImageUrl,
            ImageResponse thumbnail,
            QuestionAnswerResponse questionAnswerResponses
    ) {
        return new FeedbackDetailResponse(
                feedbackPost.getTitle(),
                feedbackPost.getId(),
                feedback.getTester().getNickname(),
                testerProfileImageUrl,
                feedback.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getRejectDetail(),
                thumbnail,
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedback.getParticipateAt(),
                feedback.getSubmitAt(),
                feedback.getResponseDeadLineAt(),
                questionAnswerResponses
        );
    }
}
