package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;

public record FeedbackPostProgressResponse(
        String title,
        String writerName,
        ImageResponse thumbnail,
        LocalDateTime startAt,
        LocalDateTime endAt,
        TargetType type,
        Integer rewardAcorn,
        List<ProjectTag> tags,
        List<FeedbackProgressResponse> feedbackProgress
) {

    public static FeedbackPostProgressResponse from(
            FeedbackPost feedbackPost,
            ImageResponse thumbnail,
            List<FeedbackProgressResponse> feedbackProgress
    ) {
        return new FeedbackPostProgressResponse(
                feedbackPost.getTitle(),
                feedbackPost.getWriterName(),
                thumbnail,
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedbackPost.getTargetType(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getProject().getTags(),
                feedbackProgress
        );
    }
}
