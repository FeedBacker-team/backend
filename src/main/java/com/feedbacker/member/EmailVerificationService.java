package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.member.dto.EmailVerificationResponse;
import com.feedbacker.member.dto.EmailVerifyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final long CODE_VALID_SECONDS = 180;        // 인증번호 유효시간 3분
    private static final long RESEND_COOLDOWN_SECONDS = 60;    // 재발송 대기 1분
    private static final long SIGNUP_WINDOW_SECONDS = 30 * 60; // 인증 후 30분 안에 가입
    private static final int MAX_ATTEMPTS = 5;                 // 인증번호 최대 입력 횟수

    private final SecureRandom random = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final MemberRepository memberRepository;
    private final EmailSender emailSender;

    /** 1-1 인증번호 발송 */
    @Transactional
    public EmailVerificationResponse sendCode(String rawEmail) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now();

        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseGet(() -> EmailVerification.of(email));

        if (!verification.canResend(now, RESEND_COOLDOWN_SECONDS)) {
            throw new CustomException(HttpStatus.TOO_MANY_REQUESTS,
                    "인증번호는 1분 후에 다시 요청할 수 있습니다.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        verification.issue(code, now, CODE_VALID_SECONDS);
        emailVerificationRepository.save(verification);

        // 발송 실패 시 예외 → 위 저장도 함께 취소됨
        emailSender.sendVerificationCode(email, code, CODE_VALID_SECONDS / 60);

        return new EmailVerificationResponse("인증번호가 발송되었습니다.", CODE_VALID_SECONDS);
    }

    /** 1-2 인증번호 확인 */
    @Transactional(noRollbackFor = CustomException.class) // 틀린 횟수는 실패해도 저장
    public EmailVerifyResponse verifyCode(String rawEmail, String code) {
        String email = normalize(rawEmail);
        LocalDateTime now = LocalDateTime.now();

        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(EmailVerificationService::invalidCode);

        if (!verification.matches(code, now, MAX_ATTEMPTS)) {
            throw invalidCode();
        }

        verification.markVerified(now, SIGNUP_WINDOW_SECONDS);
        return new EmailVerifyResponse(true, "이메일 인증이 완료되었습니다.");
    }

    /** 1-3 회원가입에서 사용: 인증 완료된 이메일인지 */
    @Transactional(readOnly = true)
    public boolean isVerified(String rawEmail) {
        return emailVerificationRepository.findByEmail(normalize(rawEmail))
                .map(v -> v.isVerifiedFor(LocalDateTime.now()))
                .orElse(false);
    }

    /** 1-3 가입 완료 후 인증 기록 삭제 */
    @Transactional
    public void consume(String rawEmail) {
        emailVerificationRepository.findByEmail(normalize(rawEmail))
                .ifPresent(emailVerificationRepository::delete);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static CustomException invalidCode() {
        return new CustomException(HttpStatus.BAD_REQUEST,
                "인증번호가 일치하지 않거나 유효시간이 만료되었습니다.");
    }
}
