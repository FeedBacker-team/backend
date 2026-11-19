package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.service.FeedbackRewardService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** 신고 제재로 인한 피드백 모집글 강제 삭제와 원복 */
@Service
@RequiredArgsConstructor
public class FeedbackPostForceDeleteService {

    private final FeedbackPostService feedbackPostService;
    private final FeedbackPostRepository feedbackPostRepository;
    private final FeedbackService feedbackService;
    private final FeedbackRewardService feedbackRewardService;
    private final FeedbackPostCompleteService feedbackPostCompleteService;

    /**
     * 모집글을 바로 삭제하고 정산한다.
     * - 제출된 피드백은 승인 처리하고 테스터에게 보상 지급
     * - 작성중 피드백은 보상 없이 취소 (슬롯 반환)
     * - 이의제기 중인 피드백은 어드민 판단에 맡기고 그대로 둔다
     * - 남은 예치 도토리는 작성자에게 환급하지 않고 소멸 (모집글은 완료 상태가 되어 자동 완료·환급 대상에서 빠진다)
     *
     * @return 소멸시킨 예치 도토리. 삭제 전에 이미 완료(정산)된 모집글이면 null
     */
    @Transactional
    public Integer forceDelete(UUID feedbackPostId) {
        // 락 순서: 모집글 -> 피드백 (accept/complete와 동일)
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostIncludingDeletedForUpdate(feedbackPostId);
        feedbackPost.delete();
        if (feedbackPost.getStatus() == FeedbackPostStatus.COMPLETED) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Feedback feedback : feedbackService.getPendingFeedbacksForUpdate(feedbackPostId)) {
            if (feedback.getStatus() == FeedbackStatus.SUBMITTED) {
                feedback.autoAccept(feedbackPost.getRewardAcorn());
                feedbackRewardService.payReward(feedback, feedbackPost);
            } else {
                feedback.cancelByForceDelete(now);
                feedbackPost.plusRemainSlotCount();
            }
        }

        int forfeited = feedbackPost.getDepositAcorn() - feedbackPostCompleteService.getPaidAcorn(feedbackPost);
        feedbackPost.complete();
        return forfeited;
    }

    /**
     * 강제 삭제한 모집글을 원복한다. 테스터에게 지급한 보상과 피드백 상태는 되돌리지 않는다.
     * 소멸시켰던 예치 도토리는 다시 모집글 예치 상태로 돌려서
     * - 모집 기간 안이면 다시 모집하고, 이후 완료될 때 정상 흐름대로 남은 예치금이 환급된다
     * - 모집 기간이 지났거나 더 받을 피드백이 없으면 바로 완료 처리해서 남은 예치금을 작성자에게 환급한다
     *
     * @param acornForfeited 강제 삭제 때 소멸시킨 예치 도토리 (null이면 삭제 전에 이미 정산된 모집글이라 삭제만 되돌림)
     */
    @Transactional
    public void restore(UUID feedbackPostId, Integer acornForfeited) {
        LocalDateTime now = LocalDateTime.now();
        FeedbackPost feedbackPost = getRestorableForUpdate(feedbackPostId, now);
        feedbackPost.restore();
        if (acornForfeited == null) {
            return;
        }

        feedbackPost.reopen(now);
        if (!now.isBefore(feedbackPost.getEndAt())) {
            feedbackPostCompleteService.complete(feedbackPost);
            return;
        }
        feedbackPostCompleteService.completeIfAllReviewed(feedbackPost);
    }

    /**
     * 원복 가능한지 미리 검증한다 (다른 원복 작업을 시작하기 전에 호출).
     * 모집글 락을 잡으므로, 이 트랜잭션이 끝날 때까지 물리 삭제가 끼어들 수 없다
     */
    @Transactional
    public void validateRestorable(UUID feedbackPostId) {
        getRestorableForUpdate(feedbackPostId, LocalDateTime.now());
    }

    /**
     * 락을 잡은 뒤 원복 기한을 검사한다. 물리 삭제도 같은 행에 락을 잡고 조건을 재검증하므로 둘은 순서대로 실행된다.
     * 행이 없으면 이미 물리 삭제된 것이므로 기한 만료로 처리한다
     */
    private FeedbackPost getRestorableForUpdate(UUID feedbackPostId, LocalDateTime now) {
        FeedbackPost feedbackPost = feedbackPostRepository.findByIdIncludingDeletedForUpdate(feedbackPostId)
                .orElseThrow(() -> new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_RESTORE_EXPIRED));
        if (feedbackPost.isRestoreExpired(now.minusDays(FeedbackPostPurgeService.RETENTION_DAYS))) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_RESTORE_EXPIRED);
        }
        return feedbackPost;
    }
}
