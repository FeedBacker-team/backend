package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.AcornChange;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResultResponse(
        UUID feedbackId,
        UUID feedbackPostId,
        String feedbackPostTitle,
        FeedbackPostStatus feedbackPostStatus,
        RejectType rejectType,
        LocalDateTime submitAt,
        LocalDateTime processedAt,
        AcornChange acornChange
) {

    public static FeedbackResultResponse from(
            Feedback feedback,
            FeedbackPost feedbackPost,
            AcornChange acornChange
    ) {
        return new FeedbackResultResponse(
                feedback.getId(),
                feedbackPost.getId(),
                feedbackPost.getTitle(),
                feedbackPost.getStatus(),
                feedback.getRejectType(),
                feedback.getSubmitAt(),
                feedback.getProcessedAt(),
                acornChange
        );
    }

}
