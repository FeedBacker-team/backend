package com.feedbacker.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(UUID memberId) {
        return createToken(memberId, ACCESS, accessTokenExpiration);
    }

    public String createRefreshToken(UUID memberId) {
        return createToken(memberId, REFRESH, refreshTokenExpiration);
    }

    /** 유효한 Access Token이면 memberId, 아니면 빈 값 */
    public Optional<UUID> parseAccessToken(String token) {
        return parse(token, ACCESS);
    }

    /** 유효한 Refresh Token이면 memberId, 아니면 빈 값 */
    public Optional<UUID> parseRefreshToken(String token) {
        return parse(token, REFRESH);
    }

    /** 응답의 expires_in 값 (초) */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    /** 쿠키 Max-Age 값 (초) */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration / 1000;
    }

    private String createToken(UUID memberId, String type, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    private Optional<UUID> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid {} token: {}", expectedType, e.getMessage());
            return Optional.empty();
        }
    }
}
