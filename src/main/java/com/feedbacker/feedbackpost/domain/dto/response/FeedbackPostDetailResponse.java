package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedbackPostDetailResponse(
        UUID feedbackPostId,
        UUID projectId,
        String title,
        String description,
        FeedbackPostStatus feedbackPostStatus,
        FeedbackStatus myFeedbackStatus,
        TargetType targetType,
        String serviceUrl,
        List<ImageResponse> images,
        Integer slotCapacity,
        Integer remainSlotCount,
        Integer depositAcorn,
        Integer rewardAcorn,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime expireAt,
        List<ProjectTag> tags,
        QuestionConfigResponse questionConfig
) {

    public static FeedbackPostDetailResponse from(
            Feedback feedback,
            FeedbackPost feedbackPost,
            List<ImageResponse> images
    ) {
        return new FeedbackPostDetailResponse(
                feedbackPost.getId(),
                feedbackPost.getProject().getId(),
                feedbackPost.getTitle(),
                feedbackPost.getDescription(),
                feedbackPost.getStatus(),
                feedback == null ? null : feedback.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getServiceUrl(),
                images,
                feedbackPost.getSlotCapacity(),
                feedbackPost.getRemainSlotCount(),
                feedbackPost.getDepositAcorn(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedback != null && feedback.getStatus() == FeedbackStatus.WRITING
                        ? feedback.getExpireAt()
                        : null,
                feedbackPost.getProject().getTags(),
                QuestionConfigResponse.from(feedbackPost)
        );
    }
}
