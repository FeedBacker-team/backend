package com.feedbacker.member;

import com.feedbacker.feedback.domain.type.AcornHistoryType;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.global.common.CustomException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcornWalletService {

    private static final int DEFAULT_ACORNS = 100;
    private static final int BONUS_ACORNS = 200;

    private final AcornWalletRepository acornWalletRepository;
    private final AcornHistoryService acornHistoryService;
    private final MemberRepository memberRepository;

    // 전체 회원 수가 이 인원 이하일 때 가입하면 도토리 2배 (팀원 계정 포함)
    @Value("${acorn.signup-bonus-limit}")
    private long signupBonusLimit;

    /** 신규 가입 시 도토리 지갑 생성 (이메일·카카오 공용) */
    @Transactional
    public AcornWallet createForNewMember(Member member) {
        // 가입한 회원이 이미 저장된 뒤 호출되므로 count()에 본인이 포함됨 (50번째 가입자까지 보너스)
        boolean bonus = memberRepository.count() <= signupBonusLimit;
        int amount = bonus ? BONUS_ACORNS : DEFAULT_ACORNS;
        AcornWallet wallet = acornWalletRepository.save(AcornWallet.create(member, amount));
        // 가입 보상도 도토리 내역에 남김 (이메일·카카오 공용)
        acornHistoryService.saveEarn(member, AcornHistoryType.SIGNUP_REWARD, amount);
        return wallet;
    }

    public void withdraw(UUID memberId, Integer acorn) {
        AcornWallet wallet = acornWalletRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "유저의 도토리 지갑을 찾을 수 없습니다."));
        wallet.withdraw(acorn);
    }

    public void earn(UUID memberId, Integer acorn) {
        AcornWallet wallet = acornWalletRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "유저의 도토리 지갑을 찾을 수 없습니다."));
        wallet.deposit(acorn);
    }
}
