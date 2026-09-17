package com.feedbacker.feedback.domain.dto;

public record AcornChange(
        Integer previousCount,
        Integer currentCount,
        Integer rewardCount
) {
}
