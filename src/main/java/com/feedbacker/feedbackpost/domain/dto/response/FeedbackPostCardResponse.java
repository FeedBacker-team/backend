package com.feedbacker.feedbackpost.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public record FeedbackPostCardResponse (
    @JsonProperty("feedback_post_id")
    UUID feedbackPostId,

    @JsonProperty("project_id")
    UUID projectId,

    @JsonProperty("project_title")
    String projectTitle,

    String title,

    @JsonProperty("target_type")
    TargetType targetType,

    FeedbackPostStatus status,

    @JsonProperty("start_at")
    LocalDateTime startAt,

    @JsonProperty("end_at")
    LocalDateTime endAt,

    @JsonProperty("slot_capacity")
    Integer slotCapacity,

    @JsonProperty("remain_slot_count")
    Integer remainSlotCount,

    @JsonProperty("reward_acorn")
    Integer rewardAcorn,

    List<ProjectTag> tags,

    @JsonProperty("thumbnail_url")
    String thumbnailUrl
)
{
    public static FeedbackPostCardResponse from(
            FeedbackPost feedbackPost,
            String thumbnailUrl
    )
    {
        return new FeedbackPostCardResponse(
                feedbackPost.getId(),
                feedbackPost.getProject().getId(),
                feedbackPost.getProject().getTitle(),
                feedbackPost.getTitle(),
                feedbackPost.getTargetType(),
                feedbackPost.getStatus(),
                feedbackPost.getStartAt(),
                feedbackPost.getEndAt(),
                feedbackPost.getSlotCapacity(),
                feedbackPost.getRemainSlotCount(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getProject().getTags(),
                thumbnailUrl
        );
    }
}
