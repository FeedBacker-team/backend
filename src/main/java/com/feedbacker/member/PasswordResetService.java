package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.member.dto.EmailVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * 2-4, 2-5 비밀번호 찾기(재설정).
 * 인증번호 저장은 회원가입용 email_verification 테이블을 같이 사용 (이미 가입한 이메일만 대상이라 겹치지 않음)
 * 가입 여부는 1-1에서도 드러나므로, 사용자가 헤매지 않게 상황별로 정확히 안내함
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long CODE_VALID_SECONDS = 180;     // 인증번호 유효시간 3분
    private static final long RESEND_COOLDOWN_SECONDS = 60; // 재발송 대기 1분
    private static final int MAX_ATTEMPTS = 5;              // 인증번호 최대 입력 횟수

    private final SecureRandom random = new SecureRandom();

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    /** 2-4 비밀번호 재설정 인증번호 발송 */
    @Transactional
    public EmailVerificationResponse sendResetCode(String rawEmail) {
        String email = EmailVerificationService.normalize(rawEmail);
        findResettableMember(email); // 가입 안 됨(404) / 소셜 가입자(400)면 여기서 안내

        LocalDateTime now = LocalDateTime.now();
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
        emailSender.sendPasswordResetCode(email, code, CODE_VALID_SECONDS / 60);
        return new EmailVerificationResponse("인증번호가 발송되었습니다.", CODE_VALID_SECONDS);
    }

    /** 2-5 비밀번호 재설정 */
    @Transactional(noRollbackFor = CustomException.class) // 틀린 횟수는 실패해도 저장
    public void resetPassword(String rawEmail, String code, String newPassword) {
        String email = EmailVerificationService.normalize(rawEmail);

        Member member = findResettableMember(email);
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(PasswordResetService::invalidCode);

        if (!verification.matches(code, LocalDateTime.now(), MAX_ATTEMPTS)) {
            throw invalidCode();
        }

        member.changePassword(passwordEncoder.encode(newPassword));
        emailVerificationRepository.delete(verification);       // 인증번호 재사용 방지
        refreshTokenRepository.deleteAllByMemberId(member.getId()); // 모든 기기 로그아웃
    }

    /**
     * 비밀번호를 재설정할 수 있는 회원: 이메일 가입자(비밀번호 있음) + 탈퇴하지 않음
     * - 가입 안 된 이메일, 탈퇴 회원 → 404
     * - 소셜(카카오) 가입자 → 400 (비밀번호가 없음)
     */
    private Member findResettableMember(String email) {
        Member member = memberRepository.findByEmail(email)
                .filter(m -> m.getDeactivatedAt() == null)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."));
        if (member.getPassword() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "카카오 로그인으로 가입된 계정입니다. 카카오 로그인을 이용해 주세요.");
        }
        return member;
    }

    private static CustomException invalidCode() {
        return new CustomException(HttpStatus.BAD_REQUEST,
                "인증번호가 일치하지 않거나 유효시간이 만료되었습니다.");
    }
}
