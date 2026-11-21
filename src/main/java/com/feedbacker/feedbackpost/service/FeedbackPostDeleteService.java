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
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.storage.SupabaseStorageService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 피드백 모집글 삭제 (신고 제재 강제 삭제·원복, 보관 기간 후 물리 삭제, 작성자 삭제).
 * 모집글 행이 지워지면 피드백의 feedback_post_id는 DB에서 NULL로 바뀌므로(ON DELETE SET NULL),
 * 물리 삭제 전에 피드백 정산을 끝낸다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackPostDeleteService {

    /** 소프트 삭제 후 물리 삭제까지의 보관 기간 */
    public static final long RETENTION_DAYS = 30L;

    private final FeedbackPostService feedbackPostService;
    private final FeedbackPostRepository feedbackPostRepository;
    private final FeedbackService feedbackService;
    private final FeedbackRewardService feedbackRewardService;
    private final FeedbackPostCompleteService feedbackPostCompleteService;
    private final SupabaseStorageService storageService;
    private final EntityManager entityManager;

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

        settlePendingFeedbacks(feedbackPost, LocalDateTime.now());

        int forfeited = feedbackPost.getDepositAcorn() - feedbackPostCompleteService.getPaidAcorn(feedbackPost);
        feedbackPost.complete();
        return forfeited;
    }

    /**
     * 작성자(회원)를 삭제하기 전에 작성자의 모든 모집글을 정산하고 물리 삭제한다 (이미지는 커밋 후 스토리지에서 삭제).
     * 회원 삭제와 같은 트랜잭션에서, 회원 삭제보다 먼저 호출한다
     * - 완료 전 모집글은 강제 삭제와 같은 방식으로 정산하고 완료 처리 (남은 예치 도토리는 환급하지 않고 소멸)
     * - 이의제기 중인 피드백은 어드민 검토 없이 인용 처리하고 보상 지급 (모집글이 물리 삭제되어 이후 검토할 수 없다).
     *   소프트 삭제된 모집글은 이미 정산(완료)됐지만 강제 삭제가 이의제기를 그대로 두므로, 이를 처리하기 위해 조회 대상에 포함한다
     */
    @Transactional
    public void deleteAllByWriter(UUID writerId) {
        LocalDateTime now = LocalDateTime.now();
        // 여러 모집글에 락을 잡으므로 id 순으로 처리해 락 순서를 고정한다
        for (UUID feedbackPostId : feedbackPostRepository.findIdsByWriterIdIncludingDeleted(writerId)) {
            // 락 순서: 모집글 -> 피드백
            FeedbackPost feedbackPost = feedbackPostRepository.findByIdIncludingDeletedForUpdate(feedbackPostId)
                    .orElse(null);
            if (feedbackPost == null) {     // 조회 이후 물리 삭제됨
                continue;
            }
            List<Feedback> settled = new ArrayList<>();
            if (feedbackPost.getStatus() != FeedbackPostStatus.COMPLETED) {
                settled.addAll(settlePendingFeedbacks(feedbackPost, now));
                feedbackPost.complete();
            }
            settled.addAll(acceptObjections(feedbackPost));

            // 정산한 피드백을 먼저 DB에 반영하고 영속성 컨텍스트에서 떼어낸다.
            // 삭제된 모집글을 참조한 채 관리 상태로 남으면, 이후 flush 때 feedback_post_id를 다시 써서 FK 위반이 날 수 있다
            entityManager.flush();
            settled.forEach(entityManager::detach);
            hardDelete(feedbackPost);
        }
    }

    public List<UUID> getPurgeableIds(LocalDateTime now) {
        return feedbackPostRepository.findPurgeableIds(
                now.minusDays(RETENTION_DAYS),
                FeedbackPostStatus.COMPLETED,
                FeedbackStatus.UNRESOLVED
        );
    }

    /**
     * 소프트 삭제 후 보관 기간이 지난 모집글 1건 물리 삭제 (스케줄러 전용).
     * 락을 잡은 뒤 조건을 다시 검증해서, 그 사이 원복/상태 변경된 글은 건너뛴다.
     * 정산이 끝났고 미해결(작성중/검토 대기/이의제기) 피드백이 없는 모집글만 지운다
     */
    @Transactional
    public boolean purge(UUID feedbackPostId, LocalDateTime now) {
        // 락 순서: 모집글 -> 피드백 (다른 정산 흐름과 동일)
        FeedbackPost feedbackPost = feedbackPostRepository.findByIdIncludingDeletedForUpdate(feedbackPostId)
                .orElse(null);
        if (feedbackPost == null || !feedbackPost.isPurgeable(now.minusDays(RETENTION_DAYS))) {
            return false;
        }
        if (feedbackService.hasUnresolvedFeedback(feedbackPostId)) {
            return false;
        }
        hardDelete(feedbackPost);
        return true;
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
        if (feedbackPost.isRestoreExpired(now.minusDays(RETENTION_DAYS))) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_RESTORE_EXPIRED);
        }
        return feedbackPost;
    }

    /**
     * 작성중·검토 대기 피드백을 정리한다. 모집글 락을 잡은 트랜잭션 안에서 호출한다
     * - 제출된 피드백은 승인 처리하고 테스터에게 보상 지급
     * - 작성중 피드백은 보상 없이 취소 (슬롯 반환)
     */
    private List<Feedback> settlePendingFeedbacks(FeedbackPost feedbackPost, LocalDateTime now) {
        List<Feedback> feedbacks = feedbackService.getPendingFeedbacksForUpdate(feedbackPost.getId());
        for (Feedback feedback : feedbacks) {
            if (feedback.getStatus() == FeedbackStatus.SUBMITTED) {
                feedback.autoAccept(feedbackPost.getRewardAcorn());
                feedbackRewardService.payReward(feedback, feedbackPost);
            } else {
                feedback.cancelByForceDelete(now);
                feedbackPost.plusRemainSlotCount();
            }
        }
        return feedbacks;
    }

    /** 이의제기 중인 피드백을 인용 처리하고 보상 지급 (작성자 삭제 정산용) */
    private List<Feedback> acceptObjections(FeedbackPost feedbackPost) {
        List<Feedback> feedbacks = feedbackService.getObjectedFeedbacksForUpdate(feedbackPost.getId());
        for (Feedback feedback : feedbacks) {
            feedback.acceptObjectionByWriterWithdrawal(feedbackPost.getRewardAcorn());
            feedbackRewardService.payReward(feedback, feedbackPost);
        }
        return feedbacks;
    }

    /** 모집글 물리 삭제 (질문은 JPA cascade로 함께 삭제). 모집글 이미지는 커밋 후 스토리지에서 지운다 */
    private void hardDelete(FeedbackPost feedbackPost) {
        UUID feedbackPostId = feedbackPost.getId();
        List<String> imagePaths = feedbackPost.getImages().stream()
                .map(ImageInfo::path)
                .filter(StringUtils::hasText)
                .toList();
        feedbackPostRepository.delete(feedbackPost);
        deleteImagesAfterCommit(feedbackPostId, imagePaths);
    }

    /**
     * 스토리지 삭제는 롤백할 수 없으므로 DB 삭제가 커밋된 뒤에 지운다.
     * 실패해도 DB 삭제는 이미 끝났으므로 로그만 남긴다 (테스터 답변 이미지는 피드백에 남아 있으므로 지우지 않는다)
     */
    private void deleteImagesAfterCommit(UUID feedbackPostId, List<String> imagePaths) {
        if (imagePaths.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.deleteImages(imagePaths);
                } catch (Exception e) {
                    log.warn("모집글 이미지 삭제 실패: feedbackPostId={}, paths={}", feedbackPostId, imagePaths, e);
                }
            }
        });
    }
}
