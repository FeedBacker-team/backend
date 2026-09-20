package com.feedbacker.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "잘못된 요청입니다. 인가 코드가 누락되었거나 잘못된 형식의 데이터입니다.")
    @JsonProperty("authorization_code")
    private String authorizationCode;
}