package com.feedbacker.report.controller;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.report.domain.dto.request.ReportCreateRequest;
import com.feedbacker.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 신고 (회원, 프로젝트 게시글, 피드백 모집글, 댓글) */
    @PostMapping
    public void report(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        reportService.report(user.getMemberId(), request);
    }
}
