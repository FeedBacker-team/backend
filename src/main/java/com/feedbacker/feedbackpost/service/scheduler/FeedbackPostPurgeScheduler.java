package com.feedbacker.feedbackpost.service.scheduler;

import com.feedbacker.feedbackpost.service.FeedbackPostPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackPostPurgeScheduler {

    private final FeedbackPostPurgeService feedbackPostPurgeService;

    // 소프트 삭제 후 보관 기간이 지난 모집글을 매일 새벽 4시에 물리 삭제
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeDeletedFeedbackPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<UUID> feedbackPostIds = feedbackPostPurgeService.getPurgeableIds(now);

        int purgedCount = 0;
        for (UUID feedbackPostId : feedbackPostIds) {
            // 모집글 단위로 트랜잭션을 분리해, 한 모집글의 실패가 다른 모집글 처리를 막지 않게 한다
            try {
                if (feedbackPostPurgeService.purge(feedbackPostId, now)) {
                    purgedCount++;
                }
            } catch (Exception e) {
                log.error("모집글 물리 삭제 실패: feedbackPostId={}", feedbackPostId, e);
            }
        }
        if (purgedCount > 0) {
            log.info("보관 기간 지난 모집글 물리 삭제: {}건", purgedCount);
        }
    }
}
