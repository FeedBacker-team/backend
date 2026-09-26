package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.feedback.domain.type.AcornHistoryType;

import java.time.LocalDateTime;

public record AcornHistoryResponse(
        AcornHistoryType type,
        LocalDateTime translateAt,
        Integer beforeAcorn,
        Integer afterAcorn,
        Integer changeAcorn
) {
    public static AcornHistoryResponse from(AcornHistory acornHistory) {
        return new AcornHistoryResponse(
                acornHistory.getType(),
                acornHistory.getTranslateAt(),
                acornHistory.getBeforeAcorn(),
                acornHistory.getAfterAcron(),
                acornHistory.getChangeAcorn()
        );
    }
}
