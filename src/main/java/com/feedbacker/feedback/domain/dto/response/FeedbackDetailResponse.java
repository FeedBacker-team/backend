package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
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

    public static FeedbackDetailResponse from(
            FeedbackPost feedbackPost,
            Feedback feedback,
            QuestionAnswerResponse questionAnswerResponses
    ) {
        return new FeedbackDetailResponse(
                feedbackPost.getTitle(),
                feedback.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getRejectDetail(),
                feedback.getParticipateAt(),
                feedback.getSubmitAt(),
                feedback.getResponseDeadLineAt(),
                questionAnswerResponses
        );
    }
}
