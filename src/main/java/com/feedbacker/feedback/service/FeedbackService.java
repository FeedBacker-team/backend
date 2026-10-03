package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.*;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.global.security.CustomUserDetails;
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
        Feedback savedFeedback = feedbackRepository.save(
                Feedback.create(feedbackPost, tester, answers)
        );
        return savedFeedback.getId();
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

    public List<Feedback> getAllFeedbacks(UUID feedbackPostId) {
        return feedbackRepository.findAllByFeedbackPostId(feedbackPostId);
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

    /** 승인/거절하지 않은 제출 피드백(SUBMITTED)이 남아 있으면 예외 (모집글 완료 시 사용) */
    public void validateNoSubmittedFeedback(UUID feedbackPostId) {
        if (hasSubmittedFeedback(feedbackPostId)) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_POST_HAS_SUBMITTED_FEEDBACK);
        }
    }

    public List<UUID> getFeedbackPostIdsWithOverdueFeedback(LocalDateTime now) {
        return feedbackRepository.findFeedbackPostIdsWithOverdue(FeedbackStatus.SUBMITTED, now);
    }

    public List<Feedback> getOverdueFeedbacksForUpdate(UUID feedbackPostId, LocalDateTime now) {
        return feedbackRepository.findOverdueForUpdate(feedbackPostId, FeedbackStatus.SUBMITTED, now);
    }

    public void validateGetFeedback(
            UUID memberId,
            Feedback feedback,
            FeedbackPost feedbackPost
    ) {
        if (!memberId.equals(feedback.getTesterId())
                && !memberId.equals(feedbackPost.getWriterId())) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

}
