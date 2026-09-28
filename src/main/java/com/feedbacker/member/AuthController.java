package com.feedbacker.member;

import com.feedbacker.member.dto.AuthResponse;
import com.feedbacker.member.dto.EmailVerificationRequest;
import com.feedbacker.member.dto.EmailVerificationResponse;
import com.feedbacker.member.dto.EmailVerifyRequest;
import com.feedbacker.member.dto.EmailVerifyResponse;
import com.feedbacker.member.dto.KakaoAuthResponse;
import com.feedbacker.member.dto.KakaoLoginRequest;
import com.feedbacker.member.dto.LoginRequest;
import com.feedbacker.member.dto.MessageResponse;
import com.feedbacker.member.dto.SignUpRequest;
import com.feedbacker.member.dto.TokenRefreshResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final KakaoAuthService kakaoAuthService;
    private final RefreshTokenCookieFactory cookieFactory;

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

    // 1-3. 이메일 회원가입 (가입 즉시 로그인)
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        AuthService.LoginResult result = authService.signUp(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }

    // 2-1. 이메일 로그인
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }

    // 3-1. 카카오 소셜 로그인 및 자동 가입
    @PostMapping("/kakao")
    public ResponseEntity<KakaoAuthResponse> loginKakao(@Valid @RequestBody KakaoLoginRequest request) {
        KakaoAuthService.KakaoLoginResult result = kakaoAuthService.login(request.authorizationCode());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }

    // 2-2. 토큰 재발급 (쿠키의 refreshToken 사용)
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    // 2-3. 로그아웃 (Access Token이 만료돼도 쿠키만으로 처리)
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expire().toString())
                .body(new MessageResponse("성공적으로 로그아웃되었습니다."));
    }
}
