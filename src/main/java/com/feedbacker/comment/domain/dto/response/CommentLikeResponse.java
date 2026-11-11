package com.feedbacker.comment.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/** 좋아요 / 좋아요 취소 결과 */
public record CommentLikeResponse(
        @JsonProperty("comment_id")
        UUID commentId,

        @JsonProperty("like_count")
        int likeCount,

        @JsonProperty("is_liked")
        boolean liked
) {
}
