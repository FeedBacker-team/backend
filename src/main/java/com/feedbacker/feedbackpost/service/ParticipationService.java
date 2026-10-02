package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.repository.FeedbackRepository;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final FeedbackRepository feedbackRepository;

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

    public Participation getParticipation(UUID feedbackPostId, UUID testerId) {
        return participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.PARTICIPATION_NOT_FOUND));
    }

    public Optional<Participation> findParticipation(UUID feedbackPostId, UUID testerId) {
        return participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId);
    }

    public void giveUp(UUID feedbackPostId, UUID testerId) {
        Participation participation = getParticipation(feedbackPostId, testerId);
        LocalDateTime now = LocalDateTime.now();
        if (!participation.isReserved()) {
            throw new BusinessException(ParticipationErrorCode.CANNOT_GIVE_UP);
        }
        if (participation.isDeadlineReached(now)) {
            throw new BusinessException(ParticipationErrorCode.SUBMISSION_DEADLINE_EXPIRED);
        }
        participation.abandon(now);
    }

    @Transactional
    public int expireOverdueParticipations() {
        LocalDateTime now = LocalDateTime.now();
        List<Participation> targets = participationRepository.findExpiredTargets(
                ParticipationStatus.RESERVED,
                now
        );
        targets.forEach(participation -> {
            participation.expire(now);

            // 수정할 예정
            Feedback feedback = feedbackRepository
                    .findByFeedbackPostIdAndTesterId(
                            participation.getFeedbackPost().getId(),
                            participation.getTester().getId())
                    .orElseThrow(RuntimeException::new);
            feedback.setStatus(FeedbackStatus.EXPIRED);
        });

        return targets.size();
    }

    public void validateNoActiveParticipant(UUID feedbackPostId) {
        if (participationRepository.existsByFeedbackPost_IdAndStatus(feedbackPostId, ParticipationStatus.RESERVED)) {
            throw new BusinessException(ParticipationErrorCode.FEEDBACK_POST_HAS_ACTIVE_PARTICIPANT);
        }
    }

    public void validateAlreadyParticipate(UUID feedbackPostId, UUID testerId) {
        participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId)
                .ifPresent(participation -> {
                    throw new BusinessException(toAlreadyParticipatedError(participation.getStatus()));
                });
    }

    private ParticipationErrorCode toAlreadyParticipatedError(ParticipationStatus status) {
        return switch (status) {
            case RESERVED -> ParticipationErrorCode.ALREADY_RESERVED;
            case SUBMITTED -> ParticipationErrorCode.ALREADY_PARTICIPATED;
            case ABANDONED -> ParticipationErrorCode.ABANDONED_PARTICIPATION;
            case EXPIRED -> ParticipationErrorCode.SUBMISSION_DEADLINE_EXPIRED;
        };
    }

    public boolean isWriting(UUID feedbackPostId, UUID testerId) {
        Optional<Participation> participation = participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId);

        if (participation.isEmpty()) {
            return false;
        }
        return true;
    }
}
