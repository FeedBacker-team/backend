package com.feedbacker.member;

import com.feedbacker.global.jwt.JwtTokenProvider;
import com.feedbacker.member.dto.AuthResponse;
import com.feedbacker.member.dto.LoginRequest;
import com.feedbacker.member.dto.SignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        // 1. 이메일 중복 검증 (409 Conflict)
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        // 2. 기본 닉네임 생성 (이메일 아이디 부분 활용)
        String defaultNickname = request.getEmail().split("@")[0];

        // 3. Member 엔티티 생성 및 비밀번호 암호화
        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(defaultNickname)
                .role(Role.OTHER)
                .authProvider(AuthProvider.EMAIL)
                .build();

        // 4. 도토리 지갑 초기화 (기본 잔액 0개)
        AcornWallet wallet = new AcornWallet(member, 0);
        member.assignWallet(wallet);

        Member savedMember = memberRepository.save(member);

        // 5. 토큰 발급 (자동 로그인 처리)
        String accessToken = jwtTokenProvider.createAccessToken(
                savedMember.getId(),
                savedMember.getEmail(),
                savedMember.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(savedMember.getId());

        return AuthResponse.builder()
                .userId(savedMember.getId())
                .email(savedMember.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. 회원 조회 및 비밀번호 일치 검증 (일치하지 않으면 401 Unauthorized)
        Member member = memberRepository.findByEmail(request.getEmail())
                .filter(m -> passwordEncoder.matches(request.getPassword(), m.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

        // 2. JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        return AuthResponse.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .build();
    }
}