package com.feedbacker.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, UUID id);

    Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /** 4-4 회원 탈퇴: 내가 본 프로젝트 조회 기록 삭제 */
    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM project_views WHERE member_id = :memberId", nativeQuery = true)
    int deleteProjectViewsByMemberId(@Param("memberId") UUID memberId);

    /** 4-4 회원 탈퇴: 내 도토리 내역 삭제 (삭제 후 영속성 컨텍스트를 비움) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM acorn_history WHERE member_id = :memberId", nativeQuery = true)
    int deleteAcornHistoriesByMemberId(@Param("memberId") UUID memberId);
}
