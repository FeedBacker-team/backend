package com.feedbacker.project.domain.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ActiveQaResponse(

        @JsonProperty("feedback_post_id")
        UUID feedbackPostId,

        String title,

        FeedbackPostStatus status,

        @JsonProperty("target_type")
        TargetType targetType,

        @JsonProperty("slot_capacity")
        Integer slotCapacity,

        @JsonProperty("remaining_slots")
        Integer remainingSlots,

        @JsonProperty("reward_acorn")
        Integer rewardAcorn,

        @JsonProperty("end_at")
        LocalDateTime endAt

) {

    public static ActiveQaResponse from(FeedbackPost feedbackPost) {
        return new ActiveQaResponse(
                feedbackPost.getId(),
                feedbackPost.getTitle(),
                feedbackPost.getStatus(),
                feedbackPost.getTargetType(),
                feedbackPost.getSlotCapacity(),
                feedbackPost.getRemainSlotCount(),
                feedbackPost.getRewardAcorn(),
                feedbackPost.getEndAt()
        );
    }
}
