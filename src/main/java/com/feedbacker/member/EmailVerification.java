package com.feedbacker.member;

import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 이메일 인증번호 (이메일당 1건, 재발송 시 덮어씀) */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification")
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    // 인증번호 만료 시각 (발송 후 3분)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 마지막 발송 시각 (재발송 쿨다운 판단용)
    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private boolean verified;

    // 인증 완료 후 회원가입 가능 기한
    private LocalDateTime verifiedUntil;

    // 틀린 횟수 (무작위 대입 방지)
    @Column(nullable = false)
    private int failedAttempts;

    private EmailVerification(String email) {
        this.email = email;
    }

    public static EmailVerification of(String email) {
        return new EmailVerification(email);
    }

    /** 새 인증번호 발급 (이전 인증 상태 초기화) */
    public void issue(String code, LocalDateTime now, long validSeconds) {
        this.code = code;
        this.sentAt = now;
        this.expiresAt = now.plusSeconds(validSeconds);
        this.verified = false;
        this.verifiedUntil = null;
        this.failedAttempts = 0;
    }

    public boolean canResend(LocalDateTime now, long cooldownSeconds) {
        return sentAt == null || !now.isBefore(sentAt.plusSeconds(cooldownSeconds));
    }

    /** 인증번호 확인. 최대 시도 횟수를 넘으면 맞게 입력해도 실패 */
    public boolean matches(String inputCode, LocalDateTime now, int maxAttempts) {
        if (failedAttempts >= maxAttempts || !now.isBefore(expiresAt)) {
            return false;
        }
        if (!code.equals(inputCode)) {
            failedAttempts++;
            return false;
        }
        return true;
    }

    public void markVerified(LocalDateTime now, long signupWindowSeconds) {
        this.verified = true;
        this.verifiedUntil = now.plusSeconds(signupWindowSeconds);
    }

    /** 회원가입 가능한 인증 완료 상태인지 */
    public boolean isVerifiedFor(LocalDateTime now) {
        return verified && verifiedUntil != null && now.isBefore(verifiedUntil);
    }
}
