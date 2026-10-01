package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.type.ParticipationStatus;
import com.feedbacker.feedbackpost.exception.ParticipationErrorCode;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Participation reservedParticipation = Participation.reserve(feedbackPost, member);
        participationRepository.save(reservedParticipation);
    }

    private void validateRemainSlot(
            List<Participation> participations,
            FeedbackPost feedbackPost
    ) {
        long occupiedSlots = participations.stream()
                .filter(Participation::occupiesSlot)
                .count();
        if (occupiedSlots >= feedbackPost.getSlotCapacity()) {
            throw new BusinessException(ParticipationErrorCode.SLOT_FULL);
        }
    }

    public void validateFeedbackSubmit(UUID feedbackPostId, UUID memberId) {
        List<Participation> participations = getAllParticipation(feedbackPostId);
        Participation participation = validateHasParticipation(participations, memberId);
        validateSubmissionDeadlineAt(participation);
        participation.setStatus(ParticipationStatus.SUBMITTED);
    }

    private Participation validateHasParticipation(
            List<Participation> participations,
            UUID memberId
    ) {
        for (Participation participation : participations) {
            if (participation.getTester().getId().equals(memberId)) {
                if (participation.getStatus() == ParticipationStatus.EXPIRED) {
                    throw new BusinessException(ParticipationErrorCode.SUBMISSION_DEADLINE_EXPIRED);
                }
                if (participation.getStatus() == ParticipationStatus.RESERVED) {
                    return participation;
                }
                throw new BusinessException(ParticipationErrorCode.ALREADY_PARTICIPATED);
            }
        }
        throw new BusinessException(ParticipationErrorCode.PARTICIPATION_NOT_FOUND);
    }

    private void validateSubmissionDeadlineAt(Participation participation) {
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(participation.getSubmissionDeadlineAt())) {
            throw new BusinessException(
                    ParticipationErrorCode.SUBMISSION_DEADLINE_EXPIRED
            );
        }
    }

    private List<Participation> getAllParticipation(UUID feedbackPostId) {
        return participationRepository.findForUpdate(feedbackPostId);
    }

    public Participation getParticipation(UUID FeedbackPostId, UUID testerId) {
        return participationRepository.findByFeedbackPost_IdAndTester_Id(FeedbackPostId, testerId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.PARTICIPATION_NOT_FOUND));
    }

    @Transactional
    public int expireOverdueParticipations() {
        LocalDateTime now = LocalDateTime.now();
        List<Participation> targets = participationRepository.findExpiredTargets(
                ParticipationStatus.RESERVED,
                now
        );
        targets.forEach(participation -> participation.expire(now));
        return targets.size();
    }
}
