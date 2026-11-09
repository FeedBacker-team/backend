package com.feedbacker.report.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.ReportSanction;
import com.feedbacker.report.domain.dto.request.ReportProcessRequest;
import com.feedbacker.report.domain.dto.request.ReportSanctionRequest;
import com.feedbacker.report.domain.dto.response.ReportDetailResponse;
import com.feedbacker.report.domain.dto.response.ReportListResponse;
import com.feedbacker.report.domain.dto.response.ReportSummaryResponse;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.exception.ReportErrorCode;
import com.feedbacker.report.repository.ReportRepository;
import com.feedbacker.report.repository.ReportSanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ReportSanctionRepository reportSanctionRepository;
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
            BigDecimal decreased = suspect.decreaseHumidity(WARNING_HUMIDITY_DECREASE);
            recordSanction(report, ReportAction.WARNING, decreased, null, null, adminId);
        }
        if (request.hasAction(ReportAction.BANNED)) {
            suspect.ban(BAN_DAYS, now);
            recordSanction(report, ReportAction.BANNED, null, BAN_DAYS, suspect.getBannedUntil(), adminId);
        }
        // 커스텀: 입력한 수치 그대로 적용
        if (request.hasAction(ReportAction.CUSTOM)) {
            BigDecimal decreased = request.humidityDecrease() == null
                    ? null
                    : suspect.decreaseHumidity(request.humidityDecrease());
            LocalDateTime banEndAt = null;
            if (request.banDays() != null) {
                suspect.ban(request.banDays(), now);
                banEndAt = suspect.getBannedUntil();
            }
            recordSanction(report, ReportAction.CUSTOM, decreased, request.banDays(), banEndAt, adminId);
        }
    }

    @Transactional
    public void deleteContent(UUID adminId, UUID reportId, ReportProcessRequest request) {
        Report report = findReport(reportId);
        report.applyAction(ReportAction.CONTENT_DELETED, adminId, memoOf(request));

        switch (report.getType()) {
            case PROJECT_POST -> findProject(report).delete();
            case FEEDBACK_POST -> findFeedbackPost(report).delete();
            case MEMBER -> throw new BusinessException(ReportErrorCode.CONTENT_DELETION_NOT_SUPPORTED);
            case COMMENT -> throw new BusinessException(ReportErrorCode.REPORT_TYPE_NOT_SUPPORTED);
        }
        recordSanction(report, ReportAction.CONTENT_DELETED, null, null, null, adminId);
        // TODO: 신고 당한 사람에게 알림 전송
    }

    /** 이 신고로 적용된 제재(습도 감소, 정지, 컨텐츠 삭제)를 모두 되돌림 */
    @Transactional
    public void revert(UUID adminId, UUID reportId, ReportProcessRequest request) {
        Report report = findReport(reportId);
        report.revert(adminId, memoOf(request));

        LocalDateTime now = LocalDateTime.now();
        List<ReportSanction> sanctions =
                reportSanctionRepository.findAllByReportIdAndRevertedAtIsNullOrderByCreatedAtDesc(reportId);
        Member suspect = null;

        for (ReportSanction sanction : sanctions) {
            if (sanction.hasHumidityDecrease() || sanction.hasBan()) {
                if (suspect == null) {
                    suspect = findSuspect(report);
                }
                if (sanction.hasHumidityDecrease()) {
                    suspect.increaseHumidity(sanction.getHumidityDecreased());
                }
                // 이미 기간이 끝난 정지는 되돌릴 게 없음
                if (sanction.hasBan() && sanction.getBanEndAt().isAfter(now)) {
                    suspect.shortenBan(sanction.getBanDays(), now);
                }
            }
            if (sanction.getAction() == ReportAction.CONTENT_DELETED) {
                restoreContent(report);
            }
            sanction.revert(adminId, now);
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

    private Project findProject(Report report) {
        return projectRepository.findById(report.getTargetId())
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
    }

    private FeedbackPost findFeedbackPost(Report report) {
        return feedbackPostRepository.findById(report.getTargetId())
                .orElseThrow(() -> new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND));
    }

    private void restoreContent(Report report) {
        switch (report.getType()) {
            case PROJECT_POST -> findProject(report).restore();
            case FEEDBACK_POST -> findFeedbackPost(report).restore();
            case COMMENT -> { } // TODO: 신고 당한 사람에게 알림 전송
            case MEMBER -> { } // 멤버는 삭제되지 않음
        }
    }

    private void recordSanction(Report report, ReportAction action, BigDecimal humidityDecreased,
                                Integer banDays, LocalDateTime banEndAt, UUID adminId) {
        reportSanctionRepository.save(ReportSanction.builder()
                .reportId(report.getId())
                .action(action)
                .humidityDecreased(humidityDecreased)
                .banDays(banDays)
                .banEndAt(banEndAt)
                .processedBy(adminId)
                .build());
    }

    private String memoOf(ReportProcessRequest request) {
        return request == null ? null : request.adminMemo();
    }
}
