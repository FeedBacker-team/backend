package com.feedbacker.comment.domain.dto.request;

import com.feedbacker.comment.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(

        @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
        @Size(max = Comment.MAX_CONTENT_LENGTH, message = "댓글은 1000자 이하여야 합니다.")
        String content

) {
}
