package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageResponse;

import java.time.LocalDateTime;

public record FeedbackDetailResponse(
        String feedbackPostTitle,
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
            ImageResponse thumbnail,
            QuestionAnswerResponse questionAnswerResponses
    ) {
        return new FeedbackDetailResponse(
                feedbackPost.getTitle(),
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
