package com.feedbacker.report.domain.dto.response;

import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.domain.type.ReportReason;
import com.feedbacker.report.domain.type.ReportType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReportDetailResponse(
        UUID reportId,
        UUID reporterId,
        UUID suspectId,
        ReportType type,
        UUID targetId,
        List<ReportReason> reasons,
        List<String> detailReasons,
        boolean processed,
        List<ReportAction> actions,
        UUID processedBy,
        String adminMemo,
        LocalDateTime processedAt,
        boolean valid,
        LocalDateTime createdAt
) {
    public static ReportDetailResponse from(Report report) {
        return new ReportDetailResponse(
                report.getId(),
                report.getReporterId(),
                report.getSuspectId(),
                report.getType(),
                report.getTargetId(),
                report.getReasons(),
                report.getDetailReasons(),
                report.isProcessed(),
                report.getActions(),
                report.getProcessedBy(),
                report.getAdminMemo(),
                report.getProcessedAt(),
                report.isValid(),
                report.getCreatedAt()
        );
    }
}
