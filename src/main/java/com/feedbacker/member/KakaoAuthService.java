package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.member.dto.KakaoAuthResponse;
import com.feedbacker.member.dto.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final KakaoClient kakaoClient;
    private final MemberRepository memberRepository;
    private final AcornWalletService acornWalletService;
    private final AuthTokenService authTokenService;

    /** 3-1 카카오 로그인 (처음이면 자동 가입) */
    @Transactional
    public KakaoLoginResult login(String authorizationCode) {
        KakaoUserInfoResponse userInfo = kakaoClient.getUserInfo(authorizationCode);
        String kakaoId = String.valueOf(userInfo.id());

        Optional<Member> existing = memberRepository.findByProviderAndProviderId(AuthProvider.KAKAO, kakaoId);
        boolean newUser = existing.isEmpty();
        Member member = existing.orElseGet(() -> signUp(kakaoId, userInfo.verifiedEmailOrNull()));

        if (member.getDeactivatedAt() != null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 시도해 주세요.");
        }

        AuthTokens tokens = authTokenService.issue(member.getId());
        KakaoAuthResponse response = new KakaoAuthResponse(
                member.getId(),
                member.getEmail(),
                tokens.accessToken(),
                tokens.accessTokenExpiresIn(),
                newUser,
                member.isProfileCompleted());
        return new KakaoLoginResult(response, tokens.refreshToken());
    }

    private Member signUp(String kakaoId, String email) {
        // 같은 이메일로 이미 이메일 가입한 계정이 있으면 막음 (계정 자동 연결은 하지 않음)
        if (email != null && memberRepository.existsByEmail(email)) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "이미 이메일로 가입된 계정입니다. 이메일로 로그인해 주세요.");
        }
        Member member = memberRepository.save(Member.createKakaoMember(kakaoId, email));
        acornWalletService.createForNewMember(member);
        return member;
    }

    public record KakaoLoginResult(KakaoAuthResponse response, String refreshToken) {
    }
}
