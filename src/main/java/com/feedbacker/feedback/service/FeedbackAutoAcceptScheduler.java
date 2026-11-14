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
public class FeedbackAutoAcceptScheduler {

    private final FeedbackService feedbackService;
    private final FeedbackFacade feedbackFacade;

    // 응답 기한(responseDeadLineAt)이 지나도록 처리되지 않은 피드백을 자동 승인
    @Scheduled(fixedDelay = 60_000)
    public void autoAcceptOverdueFeedbacks() {
        List<UUID> feedbackPostIds = feedbackService.getFeedbackPostIdsWithOverdueFeedback(LocalDateTime.now());

        int acceptedCount = 0;
        for (UUID feedbackPostId : feedbackPostIds) {
            // 모집글 단위로 트랜잭션을 분리해, 한 모집글의 실패가 다른 모집글 처리를 막지 않게 한다
            try {
                acceptedCount += feedbackFacade.autoAcceptOverdue(feedbackPostId);
            } catch (Exception e) {
                log.error("피드백 자동 승인 실패: feedbackPostId={}", feedbackPostId, e);
            }
        }
        if (acceptedCount > 0) {
            log.info("응답 기한 초과 피드백 자동 승인: {}건", acceptedCount);
        }
    }
}
