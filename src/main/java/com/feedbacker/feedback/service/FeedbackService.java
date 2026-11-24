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
import java.util.EnumSet;
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
        return feedbackRepository.findAllByTester_Id(memberId);
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
        return feedbackRepository.findAllByFeedbackPost_Id(feedbackPostId).stream()
                .filter(Feedback::isSubmitted)
                .toList();
    }

    public Feedback findMyFeedback(UUID feedbackPostId, UUID memberId) {
        return feedbackRepository
                .findByFeedbackPost_IdAndTester_Id(feedbackPostId, memberId)
                .orElse(null);
    }

    /** 모집글에서 승인된 피드백 수 (지급된 보상·환급 계산에 사용) */
    public int countAcceptedFeedbacks(UUID feedbackPostId) {
        return feedbackRepository.countByFeedbackPost_IdAndStatus(feedbackPostId, FeedbackStatus.ACCEPTED);
    }

    /** 모집글 작성자 탈퇴로 승인된 피드백이 테스터에게 있으면 true */
    public boolean hasAcceptedByWriterWithdrawal(UUID testerId) {
        return feedbackRepository.existsByTester_IdAndStatus(testerId, FeedbackStatus.ACCEPT_BY_WRITER_WITHDRAWAL);
    }

    public boolean hasSubmittedFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPost_IdAndStatus(feedbackPostId, FeedbackStatus.SUBMITTED);
    }

    public boolean hasWritingFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPost_IdAndStatus(feedbackPostId, FeedbackStatus.WRITING);
    }

    /** 작성중이거나 검토 대기 중인 피드백이 남아 있으면 true */
    public boolean hasPendingFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPost_IdAndStatusIn(feedbackPostId, FeedbackStatus.PENDING);
    }

    /** 작성중, 검토 대기, 이의제기 중인 피드백이 남아 있으면 true (모집글 물리 삭제 검증용) */
    public boolean hasUnresolvedFeedback(UUID feedbackPostId) {
        return feedbackRepository.existsByFeedbackPost_IdAndStatusIn(feedbackPostId, FeedbackStatus.UNRESOLVED);
    }

    /** 작성중이거나 검토 대기 중인 피드백을 락 걸어 조회 (모집글 강제 삭제 정산용) */
    public List<Feedback> getPendingFeedbacksForUpdate(UUID feedbackPostId) {
        return feedbackRepository.findAllByFeedbackPostIdAndStatusInForUpdate(feedbackPostId, FeedbackStatus.PENDING);
    }

    /** 이의제기 중인 피드백을 락 걸어 조회 (작성자 탈퇴 정산용) */
    public List<Feedback> getObjectedFeedbacksForUpdate(UUID feedbackPostId) {
        return feedbackRepository.findAllByFeedbackPostIdAndStatusInForUpdate(
                feedbackPostId, EnumSet.of(FeedbackStatus.OBJECTED));
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
        boolean isTester = feedback.isTester(memberId);
        boolean isWriter = memberId.equals(feedbackPost.getWriterId());
        if (!isTester && !(isWriter && feedback.isSubmitted())) {   // 제출 이후 상태(SUBMITTED/ACCEPTED/REJECTED/OBJECTED*)만
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }

    /** 해당 피드백 모집글에 이미 참여했는지 검사 */
    private void validateAlreadyParticipate(UUID feedbackPostId, UUID testerId) {
        feedbackRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId)
                .ifPresent(feedback -> {
                    throw new BusinessException(toAlreadyParticipatedError(feedback.getStatus()));
                });
    }

    private FeedbackErrorCode toAlreadyParticipatedError(FeedbackStatus status) {
        return switch (status) {
            case WRITING -> FeedbackErrorCode.ALREADY_RESERVED;
            case SUBMITTED, ACCEPTED, REJECTED,
                 OBJECTED, OBJECTION_ACCEPTED, OBJECTION_REJECTED -> FeedbackErrorCode.ALREADY_PARTICIPATED;
            case CANCELED -> FeedbackErrorCode.ABANDONED_PARTICIPATION;
            case EXPIRED -> FeedbackErrorCode.SUBMISSION_DEADLINE_EXPIRED;
            case ACCEPT_BY_WRITER_WITHDRAWAL -> FeedbackErrorCode.FEEDBACK_ACCEPTED_BY_WRITER_WITHDRAWAL;
        };
    }

    public void giveUp(UUID feedbackPostId, UUID memberId) {
        Feedback feedback = feedbackRepository.findByFeedbackPostIdAndTesterIdForUpdate(feedbackPostId, memberId)
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.PARTICIPATION_NOT_FOUND));
        feedback.cancel();
    }

    /** 테스터 탈퇴 시 정리할(작성중·이의제기) 피드백이 있는 모집글 id (id 순) */
    public List<UUID> getFeedbackPostIdsToSettleForTesterDelete(UUID testerId) {
        return feedbackRepository.findFeedbackPostIdsByTesterIdAndStatusIn(
                testerId, EnumSet.of(FeedbackStatus.WRITING, FeedbackStatus.OBJECTED));
    }

    /** 모집글에 대한 테스터의 피드백을 락 걸어 조회. 없으면 null */
    public Feedback findMyFeedbackForUpdate(UUID feedbackPostId, UUID testerId) {
        return feedbackRepository.findByFeedbackPostIdAndTesterIdForUpdate(feedbackPostId, testerId)
                .orElse(null);
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
