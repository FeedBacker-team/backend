package com.feedbacker.member;

import java.util.UUID;

/**
 * 회원이 탈퇴할 때, 회원을 지우기 직전에 발행되는 이벤트.
 * 회원과 같은 트랜잭션 안에서 실행되므로, 리스너에서 예외가 나면 탈퇴 전체가 취소된다.
 *
 * 다른 도메인은 @EventListener로 받아서 자기 데이터를 정리한다.
 * 모집글이 프로젝트를 참조하므로 아래 순서를 지켜야 한다.
 *   - @Order(1) 피드백: 참여 중인 피드백 정리, 내 모집글 정산 후 물리 삭제
 *   - @Order(2) 프로젝트: 내 프로젝트 물리 삭제
 */
public record MemberWithdrawnEvent(UUID memberId) {
}
