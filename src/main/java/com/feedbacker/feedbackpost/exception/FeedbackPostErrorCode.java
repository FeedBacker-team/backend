package com.feedbacker.feedbackpost.exception;

import com.feedbacker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackPostErrorCode implements ErrorCode {

    FEEDBACK_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백 모집글을 찾을 수 없습니다."),
    FEEDBACK_POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시글 작성자만 수행할 수 있습니다."),
    FEEDBACK_POST_NOT_RECRUITING(HttpStatus.CONFLICT, "모집 중인 게시글이 아닙니다."),
    ACTIVE_FEEDBACK_POST_EXISTS(HttpStatus.CONFLICT, "이미 모집 중인 게시글이 있습니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
