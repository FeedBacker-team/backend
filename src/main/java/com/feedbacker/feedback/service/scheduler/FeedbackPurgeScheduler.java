package com.feedbacker.feedback.service.scheduler;

import com.feedbacker.feedback.service.FeedbackPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackPurgeScheduler {

    private static final int BATCH_SIZE = 500;
    /** 한 번 실행에서 처리할 최대 묶음 수 */
    private static final int MAX_BATCHES = 20;

    private final FeedbackPurgeService feedbackPurgeService;

    // 매일 04:30, 모집글과 테스터가 모두 삭제된 피드백을 물리 삭제 (04:00 모집글 물리 삭제 이후)
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purgeOrphanFeedbacks() {
        int purgedCount = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            List<UUID> feedbackIds = feedbackPurgeService.getPurgeableIds(BATCH_SIZE);
            if (feedbackIds.isEmpty()) {
                break;
            }
            // 묶음 단위로 트랜잭션을 분리한다. 실패한 묶음은 다음 조회에서 다시 잡히므로 이번 실행은 멈추고 다음 날 재시도한다
            try {
                purgedCount += feedbackPurgeService.purge(feedbackIds);
            } catch (Exception e) {
                log.error("피드백 물리 삭제 실패: feedbackIds={}", feedbackIds, e);
                break;
            }
        }
        if (purgedCount > 0) {
            log.info("모집글·테스터가 삭제된 피드백 물리 삭제: {}건", purgedCount);
        }
    }
}
