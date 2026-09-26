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

@Service
@RequiredArgsConstructor
public class AcornHistoryService {

    private final AcornHistoryRepository acornHistoryRepository;

    @Transactional
    public void save(
            Member tester,
            Member writer,
            Feedback feedback,
            FeedbackPost feedbackPost
    ) {
        acornHistoryRepository.saveAll(
                AcornHistory.create(
                        tester,
                        writer,
                        feedback,
                        feedbackPost
                )
        );
    }

    @Transactional(readOnly = true)
    public AcornHistoryResponse getAcornHistory(CustomUserDetails user) {
        return acornHistoryRepository.findAllByMemberId(user.getMemberId());
    }
}
