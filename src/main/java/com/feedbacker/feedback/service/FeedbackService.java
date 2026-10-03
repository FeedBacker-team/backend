package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.*;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
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
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public UUID submit(
            FeedbackPost feedbackPost,
            Member tester,
            List<QuestionAnswer> answers
    ) {
        Feedback feedback = feedbackRepository
                .findByFeedbackPostIdAndTesterIdForUpdate(feedbackPost.getId(), tester.getId())
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.PARTICIPATION_NOT_FOUND));
        feedback.submit(answers);
        return feedback.getId();
    }

    @Transactional
    public void participate(FeedbackPost feedbackPost, Member tester) {
        validateAlreadyParticipate(feedbackPost.getId(), tester.getId());
        feedbackRepository.save(Feedback.create(feedbackPost, tester));
    }

    public List<Feedback> getMyFeedbacks(UUID memberId) {
        return feedbackRepository.findAllByTesterId(memberId);
    }

    public Feedback getFeedback(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackErrorCode.FEEDBACK_NOT_FOUND
                ));
    }

    public Feedback getFeedbackForUpdate(UUID feedbackId) {
        return feedbackRepository.findByIdForUpdate(feedbackId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackErrorCode.FEEDBACK_NOT_FOUND
                ));
    }

    public List<Feedback> getAllByFeedbackPostId(UUID feedbackPostId) {
        return feedbackRepository.findAllByFeedbackPostId(feedbackPostId).stream()
                .filter(Feedback::isSubmitted)
                .toList();
    }

    public Feedback findMyFeedback(UUID feedbackPostId, UUID memberId) {
        return feedbackRepository
                .findByFeedbackPostIdAndTesterId(feedbackPostId, memberId)
                .orElse(null);
    }

    /** 모집글에서 승인된 피드백 수 (지급된 보상·환급 계산에 사용) */
    public int countAcceptedFeedbacks(UUID feedbackPostId) {
        return feedbackRepository.countByFeedbackPostIdAndStatus(feedbackPostId, FeedbackStatus.ACCEPTED);
    }

    public boolean hasSubmittedFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPostIdAndStatus(feedbackPostId, FeedbackStatus.SUBMITTED);
    }

    public boolean hasWritingFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPostIdAndStatus(feedbackPostId, FeedbackStatus.WRITING);
    }

    public List<UUID> getFeedbackPostIdsWithOverdueFeedback(LocalDateTime now) {
        return feedbackRepository.findFeedbackPostIdsWithOverdue(FeedbackStatus.SUBMITTED, now);
    }

    public List<Feedback> getOverdueFeedbacksForUpdate(UUID feedbackPostId, LocalDateTime now) {
        return feedbackRepository.findOverdueForUpdate(feedbackPostId, FeedbackStatus.SUBMITTED, now);
    }

    public List<UUID> getFeedbackPostIdsWithExpiredFeedback(LocalDateTime now) {
        return feedbackRepository.findFeedbackPostIdsWithExpired(FeedbackStatus.WRITING, now);
    }

    public List<Feedback> getExpiredFeedbacksForUpdate(UUID feedbackPostId, LocalDateTime now) {
        return feedbackRepository.findExpiredForUpdate(feedbackPostId, FeedbackStatus.WRITING, now);
    }

    public void validateGetFeedback(
            UUID memberId,
            Feedback feedback,
            FeedbackPost feedbackPost
    ) {
        boolean isTester = memberId.equals(feedback.getTester().getId());
        boolean isWriter = memberId.equals(feedbackPost.getWriterId());
        if (!isTester && !(isWriter && feedback.isSubmitted())) {   // 제출 이후 상태(SUBMITTED/ACCEPTED/REJECTED)만
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

    /** 해당 피드백 모집글에 이미 참여했는지 검사 */
    private void validateAlreadyParticipate(UUID feedbackPostId, UUID testerId) {
        feedbackRepository.findByFeedbackPostIdAndTesterId(feedbackPostId, testerId)
                .ifPresent(feedback -> {
                    throw new BusinessException(toAlreadyParticipatedError(feedback.getStatus()));
                });
    }

    private FeedbackErrorCode toAlreadyParticipatedError(FeedbackStatus status) {
        return switch (status) {
            case WRITING -> FeedbackErrorCode.ALREADY_RESERVED;
            case SUBMITTED, ACCEPTED, REJECTED -> FeedbackErrorCode.ALREADY_PARTICIPATED;
            case CANCELED -> FeedbackErrorCode.ABANDONED_PARTICIPATION;
            case EXPIRED -> FeedbackErrorCode.SUBMISSION_DEADLINE_EXPIRED;
        };
    }

    public void giveUp(UUID feedbackPostId, UUID memberId) {
        Feedback feedback = feedbackRepository.findByFeedbackPostIdAndTesterIdForUpdate(feedbackPostId, memberId)
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.PARTICIPATION_NOT_FOUND));
        feedback.cancel();
    }

    public void validateExistActive(UUID feedbackPostId) {
        // '작성중' 상태인 피드백이 존재하는지 검사
        if (hasWritingFeedback(feedbackPostId)) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_POST_HAS_ACTIVE_PARTICIPANT);
        }
        // '제출' 상태인 피드백이 존재하는지 검사
        if (hasSubmittedFeedback(feedbackPostId)) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_POST_HAS_SUBMITTED_FEEDBACK);
        }
    }
}
