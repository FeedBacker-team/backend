package com.feedbacker.feedback.service;

import com.feedbacker.feedback.facade.FeedbackFacade;
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
public class FeedbackExpireScheduler {

    private final FeedbackService feedbackService;
    private final FeedbackFacade feedbackFacade;

    // 제출 기한(expireAt)이 지나도록 제출되지 않은 작성중 피드백을 만료 처리하고 슬롯을 반환
    @Scheduled(fixedDelay = 60_000)
    public void expireOverdueFeedbacks() {
        List<UUID> feedbackPostIds = feedbackService.getFeedbackPostIdsWithExpiredFeedback(LocalDateTime.now());

        int expiredCount = 0;
        for (UUID feedbackPostId : feedbackPostIds) {
            // 모집글 단위로 트랜잭션을 분리해, 한 모집글의 실패가 다른 모집글 처리를 막지 않게 한다
            try {
                expiredCount += feedbackFacade.expireOverdue(feedbackPostId);
            } catch (Exception e) {
                log.error("피드백 만료 처리 실패: feedbackPostId={}", feedbackPostId, e);
            }
        }
        if (expiredCount > 0) {
            log.info("제출 기한 초과 피드백 만료 처리: {}건", expiredCount);
        }
    }
}
