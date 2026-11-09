package com.feedbacker.report.controller;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.report.domain.dto.request.ReportProcessRequest;
import com.feedbacker.report.domain.dto.request.ReportSanctionRequest;
import com.feedbacker.report.domain.dto.response.ReportDetailResponse;
import com.feedbacker.report.domain.dto.response.ReportListResponse;
import com.feedbacker.report.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    /** 신고 목록 조회 (isProcessed 미지정 시 전체) */
    @GetMapping
    public ReportListResponse getReports(
            @RequestParam(required = false) Boolean isProcessed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminReportService.getReports(isProcessed, page, size);
    }

    /** 신고 상세 조회 */
    @GetMapping("/{reportId}")
    public ReportDetailResponse getReport(
            @PathVariable UUID reportId
    ) {
        return adminReportService.getReport(reportId);
    }

    /** 제재 (경고·밴은 함께, 커스텀은 단독) */
    @PostMapping("/{reportId}/sanction")
    public void sanction(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportSanctionRequest request
    ) {
        adminReportService.sanction(admin.getMemberId(), reportId, request);
    }

    /** 컨텐츠 삭제 */
    @PostMapping("/{reportId}/content-deletion")
    public void deleteContent(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId,
            @Valid @RequestBody(required = false) ReportProcessRequest request
    ) {
        adminReportService.deleteContent(admin.getMemberId(), reportId, request);
    }

    /** 전체 원복 (습도 감소, 정지, 컨텐츠 삭제를 모두 되돌림) */
    @PostMapping("/{reportId}/revert")
    public void revert(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId,
            @Valid @RequestBody(required = false) ReportProcessRequest request
    ) {
        adminReportService.revert(admin.getMemberId(), reportId, request);
    }

    /** 기각 처리 */
    @PostMapping("/{reportId}/rejection")
    public void reject(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId,
            @Valid @RequestBody(required = false) ReportProcessRequest request
    ) {
        adminReportService.reject(admin.getMemberId(), reportId, request);
    }
}
