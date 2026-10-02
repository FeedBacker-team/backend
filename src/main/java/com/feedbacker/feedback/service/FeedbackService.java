package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.*;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
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

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMine(UUID memberId) {
        List<Feedback> feedbacks = feedbackRepository.findAllByTesterId(memberId);
        return FeedbackResponse.fromAll(feedbacks);
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

    public List<FeedbackProgressResponse> getFeedbackProgress(UUID feedbackPostId) {
        return feedbackRepository.findAllByFeedbackPostId(feedbackPostId)
                .stream()
                .map(FeedbackProgressResponse::from)
                .toList();
    }

    public Feedback findMyFeedback(UUID feedbackPostId, UUID memberId) {
        return feedbackRepository
                .findByFeedbackPostIdAndTesterId(feedbackPostId, memberId)
                .orElse(null);
    }

    /** 승인/거절하지 않은 제출 피드백(SUBMITTED)이 남아 있으면 예외 (모집글 완료 시 사용) */
    public void validateNoSubmittedFeedback(UUID feedbackPostId) {
        if (feedbackRepository.existsByFeedbackPostIdAndStatus(feedbackPostId, FeedbackStatus.SUBMITTED)) {
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
        if (!memberId.equals(feedback.getTester().getId())
                && !memberId.equals(feedbackPost.getWriterId())) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

}
