package com.feedbacker.comment.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.comment.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommentCreateRequest(

        @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
        @Size(max = Comment.MAX_CONTENT_LENGTH, message = "댓글은 1000자 이하여야 합니다.")
        String content,

        // 답글이면 답할 댓글 아이디, 루트 댓글이면 null
        @JsonProperty("parent_id")
        UUID parentId

) {
    public boolean isReply() {
        return parentId != null;
    }
}
