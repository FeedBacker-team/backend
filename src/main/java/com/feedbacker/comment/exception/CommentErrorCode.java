package com.feedbacker.comment.exception;

import com.feedbacker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글 작성자만 수행할 수 있습니다."),
    COMMENT_NOT_ACTIVE(HttpStatus.CONFLICT, "삭제되었거나 숨김 처리된 댓글입니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "해당 게시글의 댓글에만 답글을 달 수 있습니다."),
    COMMENT_ALREADY_HIDDEN(HttpStatus.CONFLICT, "이미 숨김 처리된 댓글입니다."),
    COMMENT_NOT_HIDDEN(HttpStatus.CONFLICT, "숨김 처리된 댓글이 아닙니다."),
    COMMENT_ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 댓글입니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 누르지 않은 댓글입니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
