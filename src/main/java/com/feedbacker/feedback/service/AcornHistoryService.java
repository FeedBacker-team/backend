package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.response.AcornHistoryResponse;
import com.feedbacker.feedback.domain.type.AcornHistoryType;
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
    public List<AcornHistoryResponse> getAcornHistory(CustomUserDetails user) {
        return acornHistoryRepository
                .findAllByMemberIdOrderByTranslateAtDescIdDesc(user.getMemberId())
                .stream()
                .map(AcornHistoryResponse::from)
                .toList();
    }

    public AcornHistoryResponse getAcronHistoryByFeedbackPost(UUID memberId, UUID feedbackPostId) {
        return acornHistoryRepository.findByMemberIdAndFeedbackPostId(memberId, feedbackPostId);
    }

    @Transactional
    public void combine(UUID memberId, UUID feedbackPostId) {
        List<AcornHistory> acornHistories = acornHistoryRepository
                .findAllByMemberIdAndFeedbackPostId(memberId, feedbackPostId);
        if (acornHistories.isEmpty()) {
            return;
        }

        int totalAcorn = acornHistories.stream()
                .mapToInt(AcornHistory::getChangeAcorn)
                .reduce(0, Math::addExact);

        AcornHistory combinedHistory = AcornHistory.builder()
                .member(acornHistories.getFirst().getMember())
                .feedbackPostId(feedbackPostId)
                .feedbackId(null)
                .type(AcornHistoryType.FEEDBACK_RECRUIT)
                .changeAcorn(totalAcorn)
                .build();

        acornHistoryRepository.save(combinedHistory);
        acornHistoryRepository.deleteAll(acornHistories);
    }


}
