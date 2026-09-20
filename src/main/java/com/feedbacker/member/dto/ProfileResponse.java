package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

    @JsonProperty("user_id")
    private UUID userId;

    private String nickname;

    private String role;
}