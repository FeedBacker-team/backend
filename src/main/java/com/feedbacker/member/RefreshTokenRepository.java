package com.feedbacker.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    // 비밀번호 변경 시 모든 기기 로그아웃
    void deleteAllByMemberId(UUID memberId);
}
