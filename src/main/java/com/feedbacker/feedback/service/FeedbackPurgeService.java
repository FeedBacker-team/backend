package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 모집글과 테스터가 모두 삭제된 피드백 물리 삭제 (스케줄러 전용).
 * 두 FK는 DB에서 ON DELETE SET NULL로만 NULL이 되고 다시 채워지지 않으며,
 * 피드백 조회·변경은 모두 모집글이나 테스터를 거치므로 이 상태의 피드백에 접근하는 요청은 없다 (락 불필요)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackPurgeService {

    private final FeedbackRepository feedbackRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final SupabaseStorageService storageService;

    public List<UUID> getPurgeableIds(int limit) {
        return feedbackRepository.findOrphanIds(FeedbackStatus.UNRESOLVED, PageRequest.of(0, limit));
    }

    /**
     * 피드백을 답변과 함께 물리 삭제한다. 답변 이미지는 커밋 후 스토리지에서 지운다.
     * question_answers의 feedback FK에는 ON DELETE CASCADE가 없으므로 답변을 먼저 지운다
     *
     * @return 삭제한 피드백 수
     */
    @Transactional
    public int purge(List<UUID> feedbackIds) {
        // 조회 이후 상태가 바뀌었을 경우를 대비해 삭제 조건을 다시 확인한다
        List<UUID> targetIds = feedbackRepository.findOrphanIdsIn(feedbackIds, FeedbackStatus.UNRESOLVED);
        if (targetIds.isEmpty()) {
            return 0;
        }
        List<String> imagePaths = questionAnswerRepository.findAllByFeedbackIdIn(targetIds).stream()
                .map(QuestionAnswer::getImages)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(ImageInfo::path)
                .filter(StringUtils::hasText)
                .toList();

        questionAnswerRepository.deleteAllByFeedbackIdIn(targetIds);
        int deletedCount = feedbackRepository.deleteAllByIdIn(targetIds);
        deleteImagesAfterCommit(imagePaths);
        return deletedCount;
    }

    /** 스토리지 삭제는 롤백할 수 없으므로 DB 삭제가 커밋된 뒤에 지운다. 실패해도 로그만 남긴다 */
    private void deleteImagesAfterCommit(List<String> imagePaths) {
        if (imagePaths.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storageService.deleteImages(imagePaths);
                } catch (Exception e) {
                    log.warn("피드백 답변 이미지 삭제 실패: paths={}", imagePaths, e);
                }
            }
        });
    }
}
