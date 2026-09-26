package com.feedbacker.member;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AcornWalletService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_ACORNS = 50;
    private static final int BONUS_ACORNS = 100;

    private final AcornWalletRepository acornWalletRepository;

    // 이 날짜(한국시간)까지 가입하면 도토리 2배
    @Value("${acorn.signup-bonus-until}")
    private LocalDate signupBonusUntil;

    /** 신규 가입 시 도토리 지갑 생성 (이메일·카카오 공용) */
    @Transactional
    public AcornWallet createForNewMember(Member member) {
        boolean bonus = !LocalDate.now(KST).isAfter(signupBonusUntil);
        return acornWalletRepository.save(AcornWallet.create(member, bonus ? BONUS_ACORNS : DEFAULT_ACORNS));
    }
}
