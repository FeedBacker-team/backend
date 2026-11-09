package com.feedbacker.report.service;

import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.dto.request.ReportProcessRequest;
import com.feedbacker.report.domain.dto.request.ReportSanctionRequest;
import com.feedbacker.report.domain.dto.response.ReportDetailResponse;
import com.feedbacker.report.domain.dto.response.ReportListResponse;
import com.feedbacker.report.domain.dto.response.ReportSummaryResponse;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.exception.ReportErrorCode;
import com.feedbacker.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {

    // 경고 시 감소하는 습도
    private static final BigDecimal WARNING_HUMIDITY_DECREASE = new BigDecimal("3");
    // 밴 시 정지 일수
    private static final int BAN_DAYS = 3;

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final FeedbackPostRepository feedbackPostRepository;

    public ReportListResponse getReports(Boolean isProcessed, int page, int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new IllegalArgumentException("page는 0 이상, size는 1~100이어야 합니다.");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = isProcessed == null
                ? reportRepository.findAll(pageable)
                : reportRepository.findAllByProcessed(isProcessed, pageable);
        return ReportListResponse.from(reports.map(ReportSummaryResponse::from));
    }

    public ReportDetailResponse getReport(UUID reportId) {
        return ReportDetailResponse.from(findReport(reportId));
    }

    /** 경고·밴은 함께, 커스텀은 단독으로 적용 (조합 검증은 ReportSanctionRequest에서) */
    @Transactional
    public void sanction(UUID adminId, UUID reportId, ReportSanctionRequest request) {
        Report report = findReport(reportId);
        request.actions().forEach(action -> report.applyAction(action, adminId, request.adminMemo()));

        Member suspect = findSuspect(report);
        LocalDateTime now = LocalDateTime.now();
        if (request.hasAction(ReportAction.WARNING)) {
            suspect.decreaseHumidity(WARNING_HUMIDITY_DECREASE);
        }
        if (request.hasAction(ReportAction.BANNED)) {
            suspect.ban(BAN_DAYS, now);
        }
        // 커스텀: 입력한 수치 그대로 적용
        if (request.humidityDecrease() != null) {
            suspect.decreaseHumidity(request.humidityDecrease());
        }
        if (request.banDays() != null) {
            suspect.ban(request.banDays(), now);
        }
    }

    @Transactional
    public void deleteContent(UUID adminId, UUID reportId, ReportProcessRequest request) {
        Report report = findReport(reportId);
        report.applyAction(ReportAction.CONTENT_DELETED, adminId, memoOf(request));

        switch (report.getType()) {
            case PROJECT_POST -> projectRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND))
                    .delete();
            case FEEDBACK_POST -> feedbackPostRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND))
                    .delete();
            case MEMBER -> throw new BusinessException(ReportErrorCode.CONTENT_DELETION_NOT_SUPPORTED);
            case COMMENT -> throw new BusinessException(ReportErrorCode.REPORT_TYPE_NOT_SUPPORTED);
        }
        // TODO: 신고 당한 사람에게 알림 전송
    }

    @Transactional
    public void restoreContent(UUID adminId, UUID reportId, ReportProcessRequest request) {
        Report report = findReport(reportId);
        report.restoreContent(adminId, memoOf(request));

        switch (report.getType()) {
            case PROJECT_POST -> projectRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND))
                    .restore();
            case FEEDBACK_POST -> feedbackPostRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND))
                    .restore();
            case MEMBER -> throw new BusinessException(ReportErrorCode.CONTENT_DELETION_NOT_SUPPORTED);
            case COMMENT -> throw new BusinessException(ReportErrorCode.REPORT_TYPE_NOT_SUPPORTED);
        }
    }

    @Transactional
    public void reject(UUID adminId, UUID reportId, ReportProcessRequest request) {
        findReport(reportId).reject(adminId, memoOf(request));
    }

    private Report findReport(UUID reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));
    }

    private Member findSuspect(Report report) {
        return memberRepository.findById(report.getSuspectId())
                .orElseThrow(() -> new BusinessException(ReportErrorCode.SUSPECT_NOT_FOUND));
    }

    private String memoOf(ReportProcessRequest request) {
        return request == null ? null : request.adminMemo();
    }
}
