package com.feedbacker.member;

import com.feedbacker.member.dto.EmailVerificationRequest;
import com.feedbacker.member.dto.EmailVerificationResponse;
import com.feedbacker.member.dto.EmailVerifyRequest;
import com.feedbacker.member.dto.EmailVerifyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;

    // 1-1. 이메일 인증번호 발송
    @PostMapping("/email/verification-request")
    public ResponseEntity<EmailVerificationResponse> sendVerificationCode(
            @Valid @RequestBody EmailVerificationRequest request) {
        return ResponseEntity.ok(emailVerificationService.sendCode(request.email()));
    }

    // 1-2. 이메일 인증번호 확인
    @PostMapping("/email/verify")
    public ResponseEntity<EmailVerifyResponse> verifyCode(
            @Valid @RequestBody EmailVerifyRequest request) {
        return ResponseEntity.ok(emailVerificationService.verifyCode(request.email(), request.code()));
    }
}
