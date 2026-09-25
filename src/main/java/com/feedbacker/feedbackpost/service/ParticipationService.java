package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.exception.ParticipationErrorCode;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        List<Participation> participations = getAllParticipation(feedbackPost.getId());
        validateRemainSlot(participations, feedbackPost);
        validateIsWriter(participations, feedbackPost);

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
            throw new BusinessException(ParticipationErrorCode.SLOT_FULL);
        }
    }

    private void validateIsWriter(
            List<Participation> participations,
            FeedbackPost feedbackPost
    ) {
        for (Participation participation : participations) {
            if (participation.getTester().getId() == feedbackPost.getWriterId()) {
                throw new BusinessException(ParticipationErrorCode.SELF_PARTICIPATION_NOT_ALLOWED);
            }
        }
    }

    private void validateIsAlreadyParticipate(
            List<Participation> participations,
            UUID memberId
    ) {
        for (Participation participation : participations) {
            if (participation.getTester().getId() == memberId) {
                throw new BusinessException(ParticipationErrorCode.ALREADY_PARTICIPATED);
            }
        }
    }

    public void validateFeedbackSubmit(UUID feedbackPostId, UUID memberId) {
        List<Participation> participations = getAllParticipation(feedbackPostId);
        validateHasParticipation(participations, memberId);
        validateSubmissionDeadlineAt(participations);
    }

    private void validateHasParticipation(
            List<Participation> participations,
            UUID memberId
    ) {
        for (Participation participation : participations) {
            if (participation.getTester().getId() == memberId) {
                return;
            }
        }
        throw new BusinessException(ParticipationErrorCode.PARTICIPATION_NOT_FOUND);
    }

    private void validateSubmissionDeadlineAt(List<Participation> participations) {
        LocalDateTime now = LocalDateTime.now();

        for (Participation participation : participations) {
            if (!now.isBefore(participation.getSubmissionDeadlineAt())) {
                throw new BusinessException(
                        ParticipationErrorCode.SUBMISSION_DEADLINE_EXPIRED
                );
            }
        }
    }

    private List<Participation> getAllParticipation(UUID feedbackPostId) {
        return participationRepository.findForUpdate(feedbackPostId);
    }

    public Participation getParticipation(Member tester) {
        return participationRepository.findByTester(tester);
    }
}
