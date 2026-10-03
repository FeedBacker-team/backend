package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackPostResultResponse(
        String feedbackPostTitle,
        FeedbackPostStatus feedbackPostStatus,
        List<ImageResponse> images,
        String writerName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        TargetType type,
        List<ProjectTag> tags,
        Integer rewardAcorn,
        Integer changeAcorn,
        String serviceLink,
        List<FeedbackResultResponse> feedbackResult
) {

    public static FeedbackPostResultResponse from(
            List<FeedbackResultResponse> feedbackResult,
            FeedbackPost feedbackPost,
            Integer paidAcorn,
            List<ImageResponse> images
    ) {
        return new FeedbackPostResultResponse(
                feedbackPost.getTitle(),
                feedbackPost.getStatus(),
                images,
                feedbackPost.getWriterName(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedbackPost.getTargetType(),
                feedbackPost.getProject().getTags(),
                feedbackPost.getRewardAcorn(),
                paidAcorn,
                feedbackPost.getServiceUrl(),
                feedbackResult
        );
    }

}
