package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.repository.AcornHistoryRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcornHistoryService {

    private final AcornHistoryRepository acornHistoryRepository;

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
}
