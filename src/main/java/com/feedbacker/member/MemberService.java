package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.global.storage.SupabaseStorageService;
import com.feedbacker.member.dto.NicknameCheckResponse;
import com.feedbacker.member.dto.ProfileResponse;
import com.feedbacker.member.dto.ProfileUpdateResponse;
import com.feedbacker.member.dto.ProfileUpdateRequest;
import com.feedbacker.project.domain.ProjectTag;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^\\S{1,10}$");
    private static final int MAX_INTERESTS = 5;

    private final MemberRepository memberRepository;
    private final SupabaseStorageService storageService;

    /** 4-1 닉네임 중복 확인 (로그인 상태면 내 현재 닉네임은 사용 가능으로 처리) */
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname, UUID loginMemberId) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "닉네임은 1자 이상 10자 이내로 입력해 주세요.");
        }
        if (isNicknameTaken(nickname, loginMemberId)) {
            throw nicknameConflict();
        }
        return new NicknameCheckResponse(true, "사용 가능한 닉네임입니다.");
    }

    /** 4-2 기본 프로필 설정 */
    @Transactional
    public ProfileUpdateResponse updateProfile(UUID memberId, ProfileUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));

        Role role = parseRole(request.role());
        List<ProjectTag> interests = parseInterests(request.interests());

        if (isNicknameTaken(request.nickname(), memberId)) {
            throw nicknameConflict();
        }

        member.updateProfile(
                blankToNull(request.profileImagePath()),
                request.nickname(),
                role,
                blankToNull(request.introLink()),
                interests);

        try {
            memberRepository.flush(); // 동시에 같은 닉네임으로 저장되는 경우 여기서 걸림
        } catch (DataIntegrityViolationException e) {
            throw nicknameConflict();
        }
        return ProfileUpdateResponse.from(member, toImageUrl(member.getProfileImage()));
    }

    private boolean isNicknameTaken(String nickname, UUID excludeMemberId) {
        return excludeMemberId == null
                ? memberRepository.existsByNickname(nickname)
                : memberRepository.existsByNicknameAndIdNot(nickname, excludeMemberId);
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "직군을 올바르게 선택해 주세요.");
        }
    }

    /** 안 보내거나 빈 배열이면 빈 목록, 보내면 1~5개 + 중복 불가 */
    private List<ProjectTag> parseInterests(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        if (values.size() > MAX_INTERESTS) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "관심 분야는 최대 5개까지 선택할 수 있습니다.");
        }
        List<ProjectTag> result = new ArrayList<>();
        for (String value : values) {
            try {
                result.add(ProjectTag.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "관심 분야를 올바르게 선택해 주세요.");
            }
        }
        if (new HashSet<>(result).size() != result.size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "중복된 관심 분야를 선택할 수 없습니다.");
        }
        return result;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static CustomException nicknameConflict() {
        return new CustomException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
    }

    public Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
    }

    // 관심 분야(지연 로딩) 등을 읽으므로 트랜잭션 안에서 조회
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID memberId) {
        Member member = getMember(memberId);
        return ProfileResponse.from(member, toImageUrl(member.getProfileImage()));
    }

    /** 회원 ID → 프로필 이미지 URL (미등록 회원은 맵에 없음) */
    @Transactional(readOnly = true)
    public Map<UUID, String> getProfileImageUrls(Collection<UUID> memberIds) {
        Map<UUID, String> result = new HashMap<>();
        for (Member member : memberRepository.findAllById(memberIds)) {
            String url = toImageUrl(member.getProfileImage());
            if (url != null) {
                result.put(member.getId(), url);
            }
        }
        return result;
    }

    /** DB에 저장된 이미지 path → 프론트에 줄 URL (미등록이면 null → 프론트에서 기본 아바타) */
    public String toImageUrl(String path) {
        return StringUtils.hasText(path) ? storageService.createPublicUrl(path) : null;
    }
}