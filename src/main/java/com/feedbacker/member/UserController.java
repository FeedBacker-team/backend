package com.feedbacker.member;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final MemberService memberService;
    private final WithdrawService withdrawService;
    private final RefreshTokenCookieFactory cookieFactory;

    // 4-1. 닉네임 중복 확인 (로그인 전/후 모두 가능)
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkNickname(
            @RequestParam(required = false) String nickname,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(memberService.checkNickname(nickname, user == null ? null : user.getMemberId()));
    }

    // 4-2. 기본 프로필 설정 (로그인 필수)
    @PatchMapping("/me/profile")
    public ResponseEntity<ProfileUpdateResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(memberService.updateProfile(user.getMemberId(), request));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails user
    )  {
        return ResponseEntity.ok(memberService.getProfile(user.getMemberId()));
    }

    // 4-4. 회원 탈퇴 (로그인 필수). 성공하면 refreshToken 쿠키도 삭제
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> withdraw(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody(required = false) WithdrawRequest request) {
        withdrawService.withdraw(user.getMemberId(), request == null ? null : request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.expire().toString())
                .body(new MessageResponse("회원 탈퇴가 완료되었습니다."));
    }

    @PatchMapping("/me/interests")
    public ResponseEntity<Void> updateInterests(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody InterestsUpdateRequest request
    ) {
        memberService.updateInterests(user.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }

}
