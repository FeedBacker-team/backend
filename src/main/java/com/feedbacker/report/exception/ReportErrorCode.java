package com.feedbacker.report.exception;

import com.feedbacker.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다."),
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 대상을 찾을 수 없습니다."),
    SUSPECT_NOT_FOUND(HttpStatus.NOT_FOUND, "신고 당한 회원을 찾을 수 없습니다."),
    REPORT_TYPE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "아직 지원하지 않는 신고 유형입니다."),
    SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인은 신고할 수 없습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고한 대상입니다."),
    INVALID_REPORT_ACTION(HttpStatus.BAD_REQUEST, "올바르지 않은 신고 조치입니다."),
    ACTION_ALREADY_APPLIED(HttpStatus.CONFLICT, "이미 적용된 조치입니다."),
    REPORT_ALREADY_SANCTIONED(HttpStatus.CONFLICT, "이미 제재가 적용된 신고는 기각할 수 없습니다."),
    CONTENT_DELETION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "회원 신고는 컨텐츠 삭제 대상이 아닙니다."),
    REPORT_NOT_SANCTIONED(HttpStatus.CONFLICT, "원복할 제재가 없는 신고입니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
