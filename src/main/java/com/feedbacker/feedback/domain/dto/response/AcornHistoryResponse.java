package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.service.AcornHistory;

public record AcornHistoryResponse(
        Integer beforeAcorn,
        Integer afterAcorn,
        Integer changeAcorn
) {
    public static AcornHistoryResponse from(AcornHistory acornHistory) {
        return new AcornHistoryResponse(
                acornHistory.getBeforeAcorn(),
                acornHistory.getAfterAcron(),
                acornHistory.getChangeAcorn()
        );
    }
}
