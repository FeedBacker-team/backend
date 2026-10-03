package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.response.AcornHistoryResponse;
import com.feedbacker.feedback.repository.AcornHistoryRepository;
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
    public void saveReward(Member tester, Feedback feedback, FeedbackPost feedbackPost) {
        acornHistoryRepository.save(AcornHistory.reward(tester, feedback, feedbackPost));
    }

    @Transactional
    public void saveDeposit(Member writer, UUID feedbackPostId, Integer depositAcorn) {
        acornHistoryRepository.save(AcornHistory.deposit(writer, feedbackPostId, depositAcorn));
    }

    @Transactional
    public void saveRefund(Member writer, UUID feedbackPostId, Integer refundAcorn) {
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
