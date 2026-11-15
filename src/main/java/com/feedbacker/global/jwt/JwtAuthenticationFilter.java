package com.feedbacker.global.jwt;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
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
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 정지된 회원의 정지 종료 시각을 담는 요청 속성 (JwtAuthenticationEntryPoint에서 사용) */
    public static final String BANNED_UNTIL_ATTRIBUTE = "bannedUntil";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        // 유효한 Access Token이 있고 회원이 존재할 때만 로그인 상태로 등록
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            jwtTokenProvider.parseAccessToken(header.substring(BEARER_PREFIX.length()))
                    .flatMap(memberRepository::findById)
                    .ifPresent(member -> authenticate(request, member));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Member member) {
        // 정지된 회원은 비로그인으로 취급 (공개 API는 열람 가능, 로그인 필요 API는 엔트리 포인트에서 정지 안내)
        if (member.isBanned(LocalDateTime.now())) {
            request.setAttribute(BANNED_UNTIL_ATTRIBUTE, member.getBannedUntil());
            return;
        }
        CustomUserDetails user = new CustomUserDetails(member.getId(), member.getAuthority());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
