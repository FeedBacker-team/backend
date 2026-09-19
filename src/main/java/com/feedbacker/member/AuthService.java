package com.feedbacker.member;

import com.feedbacker.global.jwt.JwtTokenProvider;
import com.feedbacker.member.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoClient kakaoClient;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        String defaultNickname = request.getEmail().split("@")[0];

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(defaultNickname)
                .role(Role.OTHER)
                .authProvider(AuthProvider.EMAIL)
                .build();

        AcornWallet wallet = new AcornWallet(member, 0);
        member.assignWallet(wallet);

        Member savedMember = memberRepository.save(member);

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
        Member member = memberRepository.findByEmail(request.getEmail())
                .filter(m -> passwordEncoder.matches(request.getPassword(), m.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다."));

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

    @Transactional
    public KakaoAuthResponse loginKakao(KakaoLoginRequest request) {
        // 1. 카카오 토큰 발급 및 회원 정보 조회
        String kakaoToken = kakaoClient.getAccessToken(request.getAuthorizationCode());
        KakaoUserInfoResponse userInfo = kakaoClient.getUserInfo(kakaoToken);

        String email = userInfo.getKakaoAccount().getEmail();
        if (email == null || email.isBlank()) {
            email = "kakao_" + userInfo.getId() + "@feedbacker.kakao";
        }

        // 2. 가입 여부 확인
        Optional<Member> optionalMember = memberRepository.findByEmail(email);
        boolean isNewUser = optionalMember.isEmpty();
        Member member;

        if (isNewUser) {
            String nickname = (userInfo.getKakaoAccount().getProfile() != null
                    && userInfo.getKakaoAccount().getProfile().getNickname() != null)
                    ? userInfo.getKakaoAccount().getProfile().getNickname()
                    : "kakao_" + userInfo.getId();

            member = Member.builder()
                    .email(email)
                    .nickname(nickname)
                    .role(Role.OTHER)
                    .authProvider(AuthProvider.KAKAO)
                    .build();

            AcornWallet wallet = new AcornWallet(member, 0);
            member.assignWallet(wallet);
            member = memberRepository.save(member);
        } else {
            member = optionalMember.get();
        }

        // 3. 서비스 자체 JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        return KakaoAuthResponse.builder()
                .userId(member.getId())
                .email(member.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .isNewUser(isNewUser)
                .build();
    }
}