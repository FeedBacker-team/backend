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
    ALREADY_RESERVED(HttpStatus.CONFLICT, "이미 신청한 게시글입니다. 작성 중인 피드백을 이어서 진행해주세요."),
    ABANDONED_PARTICIPATION(HttpStatus.CONFLICT, "참여를 포기한 게시글에는 다시 참여할 수 없습니다."),
    SELF_PARTICIPATION_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인 게시글에는 참여할 수 없습니다."),
    SUBMISSION_DEADLINE_EXPIRED(HttpStatus.CONFLICT, "피드백 제출 기한이 만료되었습니다."),
    FEEDBACK_POST_HAS_ACTIVE_PARTICIPANT(HttpStatus.CONFLICT, "작성 중인 참여자가 있어 조기 마감할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
