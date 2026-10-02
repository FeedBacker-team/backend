package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
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
            Participation participation,
            FeedbackPost feedbackPost,
            List<ImageResponse> images
    ) {
        FeedbackStatus feedbackStatus = null;
        if (feedback != null) {
            feedbackStatus = feedback.getStatus();
        } else if (participation != null) {
            // 임시: 제출 전에는 Feedback이 없으므로 참여 상태로 판단
            if (participation.isReserved()) {
                feedbackStatus = FeedbackStatus.WRITING;
            } else if (participation.isAbandoned()) {
                feedbackStatus = FeedbackStatus.CANCELED;
            } else if (participation.isExpired()) {
                feedbackStatus = FeedbackStatus.EXPIRED;
            }
        }
        return new FeedbackPostDetailResponse(
                feedbackPost.getId(),
                feedbackPost.getProject().getId(),
                feedbackPost.getTitle(),
                feedbackPost.getDescription(),
                feedbackPost.getStatus(),
                feedbackStatus,
                feedbackPost.getTargetType(),
                feedbackPost.getServiceUrl(),
                images,
                feedbackPost.getSlotCapacity(),
                feedbackPost.getRemainSlotCount(),
                feedbackPost.getDepositAcorn(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                participation != null && participation.isReserved()
                        ? participation.getSubmissionDeadlineAt()
                        : null,
                feedbackPost.getProject().getTags(),
                QuestionConfigResponse.from(feedbackPost)
        );
    }
}
