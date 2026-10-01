package com.feedbacker.feedbackpost.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParticipationExpireScheduler {

    private final ParticipationService participationService;

    @Scheduled(fixedDelay = 60_000)
    public void expireOverdueParticipations() {
        int count = participationService.expireOverdueParticipations();
        if (count > 0) {
            log.info("제출 기한 초과 참여 만료 처리: {}건", count);
        }
    }
}
