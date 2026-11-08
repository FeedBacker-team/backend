package com.feedbacker.report.controller;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.report.domain.dto.response.ReportDetailResponse;
import com.feedbacker.report.domain.dto.response.ReportListResponse;
import com.feedbacker.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    /** 신고 목록 조회 (processed 미지정 시 전체) */
    @GetMapping
    public ReportListResponse getReports(
            @RequestParam(required = false) Boolean processed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminReportService.getReports(processed, page, size);
    }

    /** 신고 상세 조회 */
    @GetMapping("/{reportId}")
    public ReportDetailResponse getReport(
            @PathVariable UUID reportId
    ) {
        return adminReportService.getReport(reportId);
    }

    /** 경고 처리 */
    @PostMapping("/{reportId}/warn")
    public void warn(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.warn(admin.getMemberId(), reportId);
    }

    /** 밴 처리 */
    @PostMapping("/{reportId}/ban")
    public void ban(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.ban(admin.getMemberId(), reportId);
    }

    /** 커스텀 제재 */
    @PostMapping("/{reportId}/sanction")
    public void sanction(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.sanction(admin.getMemberId(), reportId);
    }

    /** 컨텐츠 삭제 */
    @PostMapping("/{reportId}/content-deletion")
    public void deleteContent(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.deleteContent(admin.getMemberId(), reportId);
    }

    /** 삭제 원상 복구 */
    @PostMapping("/{reportId}/content-restoration")
    public void restoreContent(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.restoreContent(admin.getMemberId(), reportId);
    }

    /** 기각 처리 */
    @PostMapping("/{reportId}/rejection")
    public void reject(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable UUID reportId
    ) {
        adminReportService.reject(admin.getMemberId(), reportId);
    }
}
