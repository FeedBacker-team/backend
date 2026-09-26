package com.feedbacker.member;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.dto.NicknameCheckResponse;
import com.feedbacker.member.dto.ProfileResponse;
import com.feedbacker.member.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // 4-1. 닉네임 중복 확인 (로그인 전/후 모두 가능)
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkNickname(
            @RequestParam(required = false) String nickname,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(memberService.checkNickname(nickname, user == null ? null : user.getMemberId()));
    }

    // 4-2. 기본 프로필 설정 (로그인 필수)
    @PatchMapping("/me/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(memberService.updateProfile(user.getMemberId(), request));
    }
}
