package com.feedbacker.acorn.service;

import com.feedbacker.acorn.domain.AcornHistory;
import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.acorn.domain.dto.AcornHistoryResponse;
import com.feedbacker.acorn.domain.type.AcornHistoryType;
import com.feedbacker.acorn.repository.AcornHistoryRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcornHistoryService {

    private final AcornHistoryRepository acornHistoryRepository;
    
    @Transactional
    public void recordEarn(Member member, AcornHistoryType type, Integer changeAcorn) {
        acornHistoryRepository.save(AcornHistory.earn(member, type, changeAcorn));
    }

    @Transactional
    public void recordWithdraw(Member member, AcornHistoryType type, Integer changeAcorn) {
        acornHistoryRepository.save(AcornHistory.withdraw(member, type, changeAcorn));
    }

    @Transactional
    public void recordReward(Member tester, Feedback feedback, FeedbackPost feedbackPost) {
        acornHistoryRepository.save(AcornHistory.reward(tester, feedback, feedbackPost));
    }

    @Transactional
    public void recordDeposit(Member writer, UUID feedbackPostId, Integer depositAcorn) {
        acornHistoryRepository.save(AcornHistory.deposit(writer, feedbackPostId, depositAcorn));
    }

    @Transactional
    public void recordRefund(Member writer, UUID feedbackPostId, Integer refundAcorn) {
        acornHistoryRepository.save(AcornHistory.refund(writer, feedbackPostId, refundAcorn));
    }

    @Transactional(readOnly = true)
    public List<AcornHistoryResponse> getAcornHistory(CustomUserDetails user) {
        return acornHistoryRepository
                .findAllByMemberIdOrderByTranslateAtDescIdDesc(user.getMemberId())
                .stream()
                .map(AcornHistoryResponse::from)
                .toList();
    }

}
