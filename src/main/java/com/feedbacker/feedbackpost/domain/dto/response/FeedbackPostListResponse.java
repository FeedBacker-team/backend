package com.feedbacker.feedbackpost.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import java.util.List;
public record FeedbackPostListResponse(
        @JsonProperty("total_count")
        long totalCount,

        int page,

        int size,

        @JsonProperty("has_next")
        boolean hasNext,

        @JsonProperty("feedback_posts")
        List<FeedbackPostCardResponse> feedbackPosts
) {
    public static FeedbackPostListResponse from(
            Page<FeedbackPostCardResponse> page
    ) {
        return new FeedbackPostListResponse(
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.getContent()
        );
    }
}
