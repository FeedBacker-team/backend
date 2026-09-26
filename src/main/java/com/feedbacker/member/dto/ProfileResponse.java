package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.member.Member;
import com.feedbacker.member.Role;
import com.feedbacker.project.domain.ProjectTag;

import java.util.List;
import java.util.UUID;

/** 4-2 기본 프로필 설정 응답 */
public record ProfileResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("profile_image_url") String profileImageUrl,
        String nickname,
        Role role,
        @JsonProperty("intro_link") String introLink,
        List<ProjectTag> interests,
        @JsonProperty("is_profile_completed") boolean profileCompleted
) {
    public static ProfileResponse from(Member member) {
        return new ProfileResponse(
                member.getId(),
                member.getProfileImage(),
                member.getNickname(),
                member.getRole(),
                member.getPortfolioLink(),
                member.getInterests(),
                member.isProfileCompleted());
    }
}
