package com.feedbacker.report.domain.dto.response;

import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.domain.type.ReportReason;
import com.feedbacker.report.domain.type.ReportType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 신고 목록의 한 행 */
public record ReportSummaryResponse(
        UUID reportId,
        UUID reporterId,
        UUID suspectId,
        ReportType type,
        UUID targetId,
        List<ReportReason> reasons,
        boolean processed,
        List<ReportAction> actions,
        boolean valid,
        LocalDateTime createdAt
) {
    public static ReportSummaryResponse from(Report report) {
        return new ReportSummaryResponse(
                report.getId(),
                report.getReporterId(),
                report.getSuspectId(),
                report.getType(),
                report.getTargetId(),
                report.getReasons(),
                report.isProcessed(),
                report.getActions(),
                report.isValid(),
                report.getCreatedAt()
        );
    }
}
