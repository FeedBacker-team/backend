package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.ImageType;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.project.domain.ProjectTag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeedbackSimpleResponse(
        UUID id,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        TargetType type,
        FeedbackPostStatus status,
        Integer rewardAcorn,
        Integer slotCapacity,
        Integer remainSlot,
        List<ProjectTag> tags,
        ImageResponse thumbnailInfo
) {

    public static FeedbackSimpleResponse from(FeedbackPost feedbackPost, ImageResponse thumbnailInfo) {
        return new FeedbackSimpleResponse(
                feedbackPost.getId(),
                feedbackPost.getTitle(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedbackPost.getTargetType(),
                feedbackPost.getStatus(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getSlotCapacity(),
                feedbackPost.getRemainSlotCount(),
                feedbackPost.getProject().getTags(),
                thumbnailInfo
        );
    }
}
