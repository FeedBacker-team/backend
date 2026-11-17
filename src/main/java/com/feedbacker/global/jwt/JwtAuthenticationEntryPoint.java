package com.feedbacker.global.jwt;

import com.feedbacker.member.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 로그인이 필요한 API에 토큰 없이/잘못된 토큰으로 접근했을 때 401 응답.
 * 정지된 회원이면 403과 정지 안내 응답.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final DateTimeFormatter BANNED_UNTIL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        if (request.getAttribute(JwtAuthenticationFilter.BANNED_UNTIL_ATTRIBUTE) instanceof LocalDateTime bannedUntil) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (Member.isPermanentBanUntil(bannedUntil)) {
                response.getWriter().write("{\"message\": \"영구 정지된 계정입니다.\"}");
                return;
            }
            response.getWriter().write("{\"message\": \"이용이 정지된 계정입니다. ("
                    + bannedUntil.format(BANNED_UNTIL_FORMAT) + "까지)\"}");
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"message\": \"인증이 필요한 요청입니다. 다시 로그인해 주세요.\"}");
    }
}
