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
    INVALID_QUESTION_ANSWER(HttpStatus.BAD_REQUEST, "올바른 질문 답변을 입력해주세요."),
    DUPLICATE_QUESTION_ANSWER(HttpStatus.BAD_REQUEST, "같은 질문에 중복으로 답변할 수 없습니다."),
    QUESTION_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "질문 유형에 맞는 답변을 제출해주세요."),
    INVALID_SELECTED_OPTION(HttpStatus.BAD_REQUEST, "질문에 존재하는 보기 번호를 선택해주세요."),
    DUPLICATE_SELECTED_OPTION(HttpStatus.BAD_REQUEST, "같은 보기를 중복으로 선택할 수 없습니다."),
    MAX_SELECTION_EXCEEDED(HttpStatus.BAD_REQUEST, "최대 선택 개수를 초과했습니다."),
    ANSWER_TOO_SHORT(HttpStatus.BAD_REQUEST, "주관식 답변의 최소 글자 수를 충족해주세요."),
    REQUIRED_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "필수 질문에 답변해주세요."),
    FEEDBACK_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 피드백입니다."),
    FEEDBACK_NOT_SUBMITTED(HttpStatus.CONFLICT, "제출된 피드백만 처리할 수 있습니다."),
    FEEDBACK_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 피드백입니다."),
    FEEDBACK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 피드백에 접근할 권한이 없습니다."),
    FEEDBACK_NOT_REJECTED(HttpStatus.CONFLICT, "거절된 피드백만 이의제기할 수 있습니다."),
    OBJECTION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 이의제기를 신청한 피드백입니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
