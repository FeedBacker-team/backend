package com.feedbacker.feedbackpost.exception;

import com.feedbacker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ParticipationErrorCode implements ErrorCode {

    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "참여 정보를 찾을 수 없습니다."),
    SLOT_FULL(HttpStatus.CONFLICT, "참여 가능한 슬롯이 없습니다."),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "이미 참여한 게시글입니다."),
    SELF_PARTICIPATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인 게시글에는 참여할 수 없습니다."),
    SUBMISSION_DEADLINE_EXPIRED(HttpStatus.CONFLICT, "피드백 제출 기한이 만료되었습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
