package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageInfo;
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
        String objectReason,
        String objectResult,
        ImageResponse Thumbnail,
        LocalDateTime startAt,
        LocalDateTime endaAt,
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
            String testerProfileImageUrl,
            ImageResponse thumbnail,
            QuestionAnswerResponse questionAnswerResponses
    ) {
        return new FeedbackDetailResponse(
                feedbackPost.getTitle(),
                feedbackPost.getId(),
                feedback.getTesterName(),
                testerProfileImageUrl,
                feedback.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getRewardAcorn(),
                feedback.getRejectType(),
                feedback.getRejectDetail(),
                feedback.getObjectReason(),
                feedback.getObjectResult(),
                thumbnail,
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                participation.getReservedAt(),
                feedback.getSubmitAt(),
                feedback.getSubmitAt().plusHours(RESPONSE_DEADLINE),
                questionAnswerResponses
        );
    }
}
