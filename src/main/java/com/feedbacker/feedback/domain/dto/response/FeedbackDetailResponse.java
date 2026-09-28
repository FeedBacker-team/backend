package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.type.TargetType;

import java.time.LocalDateTime;

public record FeedbackDetailResponse(
        String feedbackPostTitle,
        FeedbackStatus feedbackStatus,
        TargetType targetType,
        int rewardAcorn,
        RejectType rejectType,
        String rejectDetail,
        LocalDateTime participateAt,
        LocalDateTime submitAt,
        LocalDateTime responseDeadlineAt,
        QuestionAnswerResponse questionAnswerResponses
) {

    private static final int RESPONSE_DEADLINE = 72;

    public static FeedbackDetailResponse from(
            FeedbackPost feedbackPost,
            Feedback feedback,
            Participation participation,
            QuestionAnswerResponse questionAnswerResponses
    ) {
        return new FeedbackDetailResponse(
                feedbackPost.getTitle(),
                feedback.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getRejectDetail(),
                participation.getReservedAt(),
                feedback.getSubmitAt(),
                feedback.getSubmitAt().plusHours(RESPONSE_DEADLINE),
                questionAnswerResponses
        );
    }
}
