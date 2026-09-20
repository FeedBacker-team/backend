package com.feedbacker.member;

import com.feedbacker.member.dto.KakaoTokenResponse;
import com.feedbacker.member.dto.KakaoUserInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoClient {

    private final RestClient restClient;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    public KakaoClient() {
        this.restClient = RestClient.create();
    }

    public String getAccessToken(String authorizationCode) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("redirect_uri", redirectUri);
            params.add("code", authorizationCode);

            KakaoTokenResponse response = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new BadCredentialsException("카카오 인증에 실패했습니다. 다시 시도해 주세요.");
            }
            return response.getAccessToken();
        } catch (Exception e) {
            throw new BadCredentialsException("카카오 인증에 실패했습니다. 다시 시도해 주세요.");
        }
    }

    public KakaoUserInfoResponse getUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.getKakaoAccount() == null) {
                throw new BadCredentialsException("카카오 인증에 실패했습니다. 다시 시도해 주세요.");
            }
            return response;
        } catch (Exception e) {
            throw new BadCredentialsException("카카오 인증에 실패했습니다. 다시 시도해 주세요.");
        }
    }
}