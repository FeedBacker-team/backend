package com.feedbacker.member;

import com.feedbacker.member.dto.ProfileResponse;
import com.feedbacker.member.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public ProfileResponse updateProfile(UUID memberId, ProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 기존 본인 닉네임과 다른데 이미 존재하는 닉네임일 경우 409 Conflict 발생
        if (!member.getNickname().equals(request.getNickname())
                && memberRepository.existsByNickname(request.getNickname())) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        // 요청받은 직군(developer/designer)을 Enum으로 변환
        Role role = Role.valueOf(request.getRole().toUpperCase());

        // 닉네임 및 직군 갱신
        member.setInitialProfile(request.getNickname(), role);

        return ProfileResponse.builder()
                .userId(member.getId())
                .nickname(member.getNickname())
                .role(member.getRole().name().toLowerCase())
                .build();
    }
}