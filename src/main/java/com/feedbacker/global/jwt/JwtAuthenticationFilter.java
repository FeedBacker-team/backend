package com.feedbacker.global.jwt;

import com.feedbacker.global.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 유효한 Access Token이 있을 때만 로그인 상태로 등록
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            jwtTokenProvider.parseAccessToken(header.substring(BEARER_PREFIX.length()))
                    .ifPresent(memberId -> {
                        CustomUserDetails user = new CustomUserDetails(memberId);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                    });
        }

        filterChain.doFilter(request, response);
    }
}
