package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.facade.FeedbackPostFacade;
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
public class FeedbackPostAutoCompleteScheduler {

    private final FeedbackPostService feedbackPostService;
    private final FeedbackPostFacade feedbackPostFacade;

    // 모집 기간(endAt)이 지나도록 완료되지 않은 모집글을 자동 완료·환급
    @Scheduled(fixedDelay = 60_000)
    public void autoCompleteEndedFeedbackPosts() {
        List<UUID> feedbackPostIds = feedbackPostService.getEndedFeedbackPostIds(LocalDateTime.now());

        int completedCount = 0;
        for (UUID feedbackPostId : feedbackPostIds) {
            // 모집글 단위로 트랜잭션을 분리해, 한 모집글의 실패가 다른 모집글 처리를 막지 않게 한다
            try {
                if (feedbackPostFacade.autoCompleteEnded(feedbackPostId)) {
                    completedCount++;
                }
            } catch (Exception e) {
                log.error("모집글 자동 완료 실패: feedbackPostId={}", feedbackPostId, e);
            }
        }
        if (completedCount > 0) {
            log.info("모집 기간 종료 모집글 자동 완료: {}건", completedCount);
        }
    }
}
