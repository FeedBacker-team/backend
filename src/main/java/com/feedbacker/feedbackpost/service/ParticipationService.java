package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;

    public void createParticipation(
            FeedbackPost feedbackPost,
            Member member
    ) {
        List<Participation> participations = getParticipations(feedbackPost.getId());
        validateRemainSlot(participations, feedbackPost);
        validateWriter(participations, feedbackPost);

        Participation reservedParticipation = Participation.reserve(
                feedbackPost,
                member
        );

        participationRepository.save(reservedParticipation);
    }

    private void validateRemainSlot(
            List<Participation> participations,
            FeedbackPost feedbackPost
    ) {
        if (participations.size() >= feedbackPost.getSlotCapacity()) {
            throw new BusinessException(FeedbackPostErrorCode.SLOT_FULL);
        }
    }

    private void validateWriter(
            List<Participation> participations,
            FeedbackPost feedbackPost
    ) {
        for (Participation participation : participations) {
            if (participation.getTester().getId() == feedbackPost.getWriterId()) {
                throw new BusinessException(FeedbackPostErrorCode.SELF_PARTICIPATION_NOT_ALLOWED);
            }
        }
    }

    public void validateAccessAuth(UUID memberId) {
        List<Participation> participations = getParticipations(feedbackPost.getId());
        for (Participation participation : participations) {
            if (participation.getTester().getId() == memberId) {
                throw new BusinessException(FeedbackPostErrorCode.ALREADY_PARTICIPATED);
            }
        }
    }

    private List<Participation> getParticipations(UUID feedbackPostId) {
        return participationRepository.findForUpdate(feedbackPostId);
    }
}
