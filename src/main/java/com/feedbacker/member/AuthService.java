package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.global.jwt.JwtTokenProvider;
import com.feedbacker.member.dto.AuthResponse;
import com.feedbacker.member.dto.TokenRefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_ACORNS = 50;
    private static final int BONUS_ACORNS = 100;

    private final MemberRepository memberRepository;
    private final AcornWalletRepository acornWalletRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final AuthTokenService authTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 이 날짜(한국시간)까지 가입하면 도토리 2배
    @Value("${acorn.signup-bonus-until}")
    private LocalDate signupBonusUntil;

    /** 1-3 이메일 회원가입 (가입 즉시 로그인) */
    @Transactional
    public LoginResult signUp(String rawEmail, String password) {
        String email = EmailVerificationService.normalize(rawEmail);

        if (memberRepository.existsByEmail(email) || !emailVerificationService.isVerified(email)) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "이메일 인증이 완료되지 않았거나 이미 존재하는 계정입니다.");
        }

        Member member = memberRepository.save(
                Member.createEmailMember(email, passwordEncoder.encode(password)));
        createWallet(member);
        emailVerificationService.consume(email);

        return loginResult(member);
    }

    /** 2-1 이메일 로그인 */
    @Transactional
    public LoginResult login(String rawEmail, String password) {
        String email = EmailVerificationService.normalize(rawEmail);

        Member member = memberRepository.findByEmail(email)
                .filter(m -> m.getPassword() != null)          // 카카오 가입자는 비밀번호 없음
                .filter(m -> m.getDeactivatedAt() == null)     // 탈퇴 회원 제외
                .filter(m -> passwordEncoder.matches(password, m.getPassword()))
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 일치하지 않습니다."));

        return loginResult(member);
    }

    /** 2-2 Access Token 재발급 (Refresh Token은 그대로 유지) */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw invalidRefreshToken();
        }

        UUID memberId = jwtTokenProvider.parseRefreshToken(refreshToken)
                .orElseThrow(AuthService::invalidRefreshToken);

        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .filter(t -> t.getMemberId().equals(memberId))
                .filter(t -> !t.isExpired(LocalDateTime.now()))
                .orElseThrow(AuthService::invalidRefreshToken);

        Member member = memberRepository.findById(saved.getMemberId())
                .filter(m -> m.getDeactivatedAt() == null)
                .orElseThrow(AuthService::invalidRefreshToken);

        return new TokenRefreshResponse(
                jwtTokenProvider.createAccessToken(member.getId()),
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                member.isProfileCompleted());
    }

    /** 2-3 로그아웃: 서버에 저장된 Refresh Token 삭제 (없어도 성공 처리) */
    @Transactional
    public void logout(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }
    }

    /** 가입 시 도토리 지갑 생성 (카카오 가입에서도 사용) */
    void createWallet(Member member) {
        boolean bonus = !LocalDate.now(KST).isAfter(signupBonusUntil);
        acornWalletRepository.save(AcornWallet.create(member, bonus ? BONUS_ACORNS : DEFAULT_ACORNS));
    }

    private LoginResult loginResult(Member member) {
        AuthTokens tokens = authTokenService.issue(member.getId());
        AuthResponse response = new AuthResponse(
                member.getId(),
                member.getEmail(),
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                member.isProfileCompleted());
        return new LoginResult(response, tokens.refreshToken());
    }

    private static CustomException invalidRefreshToken() {
        return new CustomException(HttpStatus.UNAUTHORIZED,
                "유효하지 않거나 만료된 리프레시 토큰입니다. 다시 로그인해 주세요.");
    }

    /** 컨트롤러로 넘길 결과 (응답 바디 + 쿠키에 넣을 Refresh Token) */
    public record LoginResult(AuthResponse response, String refreshToken) {
    }
}
