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
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** 카카오 서버와 통신 (인가 코드 → 토큰 → 사용자 정보) */
@Slf4j
@Component
public class KakaoClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String defaultRedirectUri;
    private final Set<String> allowedRedirectUris;

    public KakaoClient(
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.client-secret}") String clientSecret,
            @Value("${kakao.redirect-uri}") String defaultRedirectUri,
            @Value("${kakao.allowed-redirect-uris}") String allowedRedirectUris) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.defaultRedirectUri = defaultRedirectUri;
        // 쉼표로 구분된 허용 목록 + 기본값은 항상 허용
        this.allowedRedirectUris = Arrays.stream(allowedRedirectUris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        this.allowedRedirectUris.add(defaultRedirectUri);
    }

    public KakaoUserInfoResponse getUserInfo(String authorizationCode, String redirectUri) {
        String kakaoAccessToken = requestAccessToken(authorizationCode, resolveRedirectUri(redirectUri));
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

    /** 요청에 redirect_uri가 없으면 기본값, 있으면 허용 목록에 있는 값만 사용 */
    private String resolveRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri)) {
            return defaultRedirectUri;
        }
        if (!allowedRedirectUris.contains(redirectUri)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "허용되지 않은 redirect_uri입니다.");
        }
        return redirectUri;
    }

    private String requestAccessToken(String authorizationCode, String redirectUri) {
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
        } catch (RestClientResponseException e) {
            // 카카오가 돌려준 실제 오류(예: invalid_grant, KOE006)를 로그에 남김
            log.warn("Kakao token request failed: status={}, body={}, redirectUri={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), redirectUri);
            throw kakaoFailed();
        } catch (RestClientException e) {
            log.warn("Kakao token request failed: {}", e.getMessage());
            throw kakaoFailed();
        }
    }

    private static CustomException kakaoFailed() {
        return new CustomException(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 시도해 주세요.");
    }
}
