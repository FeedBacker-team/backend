package com.feedbacker.feedback.exception;

import com.feedbacker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedbackErrorCode implements ErrorCode {

    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    FEEDBACK_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 피드백입니다."),
    FEEDBACK_NOT_SUBMITTED(HttpStatus.CONFLICT, "제출된 피드백만 처리할 수 있습니다."),
    FEEDBACK_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 피드백입니다."),
    FEEDBACK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 피드백에 접근할 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
