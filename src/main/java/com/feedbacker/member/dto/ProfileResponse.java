package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.member.Member;
import com.feedbacker.member.Role;
import com.feedbacker.project.domain.ProjectTag;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("profile_image_url") String profileImageUrl,
        String nickname,
        Role role,
        @JsonProperty("intro_link") String introLink,
        List<ProjectTag> interests,
        @JsonProperty("is_profile_completed") boolean profileCompleted,
        Integer acorn,
        BigDecimal humidity
) {

    public static ProfileResponse from(Member member, String profileImageUrl) {
        return new ProfileResponse(
                member.getId(),
                profileImageUrl,
                member.getNickname(),
                member.getRole(),
                member.getPortfolioLink(),
                member.getInterests(),
                member.isProfileCompleted(),
                member.getAcornWallet().getBalance(),
                member.getHumidity()
        );
    }
}
