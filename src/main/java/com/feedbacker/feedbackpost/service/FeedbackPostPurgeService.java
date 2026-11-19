package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 소프트 삭제된 모집글의 물리 삭제.
 * 모집글 행이 지워지면 피드백의 feedback_post_id는 DB에서 NULL로 바뀌므로(ON DELETE SET NULL),
 * 정산이 끝났고 미해결(작성중/검토 대기/이의제기) 피드백이 없는 모집글만 지운다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackPostPurgeService {

    /** 소프트 삭제 후 물리 삭제까지의 보관 기간 */
    public static final long RETENTION_DAYS = 30L;

    private final FeedbackPostRepository feedbackPostRepository;
    private final FeedbackService feedbackService;
    private final SupabaseStorageService storageService;

    public List<UUID> getPurgeableIds(LocalDateTime now) {
        return feedbackPostRepository.findPurgeableIds(
                now.minusDays(RETENTION_DAYS),
                FeedbackPostStatus.COMPLETED,
                FeedbackStatus.UNRESOLVED
        );
    }

    /** 모집글 1건 물리 삭제. 락을 잡은 뒤 조건을 다시 검증해서, 그 사이 원복/상태 변경된 글은 건너뛴다 */
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
        List<String> imagePaths = feedbackPost.getImages().stream()
                .map(ImageInfo::path)
                .filter(StringUtils::hasText)
                .toList();
        feedbackPostRepository.delete(feedbackPost);
        deleteImagesAfterCommit(feedbackPostId, imagePaths);
        return true;
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
