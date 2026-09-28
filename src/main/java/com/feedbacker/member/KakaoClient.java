package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import com.feedbacker.member.dto.KakaoTokenResponse;
import com.feedbacker.member.dto.KakaoUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** 카카오 서버와 통신 (인가 코드 → 토큰 → 사용자 정보) */
@Slf4j
@Component
public class KakaoClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoClient(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret}") String clientSecret,
            @Value("${kakao.redirect-uri}") String redirectUri) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public KakaoUserInfoResponse getUserInfo(String authorizationCode) {
        String kakaoAccessToken = requestAccessToken(authorizationCode);
        try {
            KakaoUserInfoResponse userInfo = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
            if (userInfo == null || userInfo.id() == null) {
                throw kakaoFailed();
            }
            return userInfo;
        } catch (RestClientException e) {
            log.warn("Kakao user info request failed: {}", e.getMessage());
            throw kakaoFailed();
        }
    }

    private String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }
        try {
            KakaoTokenResponse token = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (token == null || !StringUtils.hasText(token.accessToken())) {
                throw kakaoFailed();
            }
            return token.accessToken();
        } catch (RestClientException e) {
            log.warn("Kakao token request failed: {}", e.getMessage());
            throw kakaoFailed();
        }
    }

    private static CustomException kakaoFailed() {
        return new CustomException(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 시도해 주세요.");
    }
}
