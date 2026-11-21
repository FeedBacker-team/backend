package com.feedbacker.report.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

public record ReportListResponse(
        @JsonProperty("total_count")
        long totalCount,

        int page,

        int size,

        @JsonProperty("has_next")
        boolean hasNext,

        List<ReportSummaryResponse> reports
) {
    public static ReportListResponse from(Page<ReportSummaryResponse> page) {
        return new ReportListResponse(
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.getContent()
        );
    }
}
