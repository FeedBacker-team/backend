package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Gmail SMTP로 메일 발송 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVerificationCode(String to, String code, long validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[FeedBacker] 이메일 인증번호 안내");
        message.setText("""
                안녕하세요, FeedBacker입니다.

                아래 인증번호를 입력해 주세요.

                인증번호: %s

                인증번호는 %d분 동안 유효합니다.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(code, validMinutes));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}", to, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}
