package com.feedbacker.member;

import com.feedbacker.global.common.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 4-4 회원 탈퇴.
 * 유예 기간 없이 바로 물리 삭제한다. 남은 도토리는 소멸한다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawService {

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailVerificationRepository emailVerificationRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;
	private final KakaoClient kakaoClient;

	@Transactional
	public void withdraw(UUID memberId, String password) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."));
		verifyPassword(member, password);

		AuthProvider provider = member.getProvider();
		String providerId = member.getProviderId();
		String email = member.getEmail();

		// 1. 다른 도메인 정리 (피드백·모집글 정산/삭제, 프로젝트 삭제). 같은 트랜잭션에서 실행된다
		eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));

		// 2. 회원을 참조하는 내 데이터 삭제
		//    (정산 중에 도토리 내역이 새로 생길 수 있어서, 엔티티 cascade 대신 DB에서 한 번에 지운다)
		memberRepository.deleteProjectViewsByMemberId(memberId);
		memberRepository.deleteAcornHistoriesByMemberId(memberId);
		refreshTokenRepository.deleteAllByMemberId(memberId);
		if (email != null) {
			emailVerificationRepository.findByEmail(email).ifPresent(emailVerificationRepository::delete);
		}

		// 3. 회원 삭제 (도토리 지갑, 관심 분야는 함께 삭제됨)
		//    위에서 영속성 컨텍스트를 비웠으므로 다시 조회해서 지운다
		memberRepository.findById(memberId).ifPresent(memberRepository::delete);

		// 4. 카카오 연결 끊기는 DB 삭제가 확정된 뒤에 한다 (외부 호출은 롤백할 수 없으므로)
		if (provider == AuthProvider.KAKAO && StringUtils.hasText(providerId)) {
			unlinkKakaoAfterCommit(providerId);
		}
	}

	/** 이메일 가입자만 비밀번호 확인 (카카오 가입자는 비밀번호가 없음) */
	private void verifyPassword(Member member, String password) {
		if (member.getPassword() == null) {
			return;
		}
		if (!StringUtils.hasText(password)) {
			throw new CustomException(HttpStatus.BAD_REQUEST, "비밀번호를 입력해 주세요.");
		}
		// 401을 쓰면 프론트가 토큰 만료로 착각해 재발급을 시도할 수 있어서 400으로 응답
		if (!passwordEncoder.matches(password, member.getPassword())) {
			throw new CustomException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
		}
	}

	private void unlinkKakaoAfterCommit(String kakaoId) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				kakaoClient.unlink(kakaoId);
			}
		});
	}
}
