package com.feedbacker.global.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 직접 던지는 비즈니스 예외.
 * 예) throw new CustomException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
 */
@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
