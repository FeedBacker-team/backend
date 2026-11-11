package com.feedbacker.comment.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 답글 목록 (커서 방식). 다음 페이지는 마지막 답글의 (created_at, comment_id) 를 커서로 요청한다.
 */
public record CommentReplyListResponse(
        List<CommentResponse> replies,

        @JsonProperty("has_next")
        boolean hasNext,

        @JsonProperty("next_cursor_created_at")
        LocalDateTime nextCursorCreatedAt,

        @JsonProperty("next_cursor_id")
        UUID nextCursorId
) {
    public static CommentReplyListResponse of(List<CommentResponse> replies, boolean hasNext) {
        if (!hasNext || replies.isEmpty()) {
            return new CommentReplyListResponse(replies, false, null, null);
        }
        CommentResponse last = replies.get(replies.size() - 1);
        return new CommentReplyListResponse(replies, true, last.createdAt(), last.commentId());
    }
}
