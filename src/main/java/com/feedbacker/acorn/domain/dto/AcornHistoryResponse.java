package com.feedbacker.acorn.domain.dto;

import com.feedbacker.acorn.domain.AcornHistory;
import com.feedbacker.acorn.domain.type.AcornHistoryType;

import java.time.LocalDateTime;

public record AcornHistoryResponse(
        AcornHistoryType type,
        LocalDateTime translateAt,
        Integer changeAcorn
) {
    public static AcornHistoryResponse from(AcornHistory acornHistory) {
        return new AcornHistoryResponse(
                acornHistory.getType(),
                acornHistory.getTranslateAt(),
                acornHistory.getChangeAcorn()
        );
    }
}
