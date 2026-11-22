package com.feedbacker.report.service;

import com.feedbacker.feedbackpost.service.FeedbackPostForceDeleteService;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.ReportSanction;
import com.feedbacker.report.domain.dto.request.ReportProcessRequest;
import com.feedbacker.report.domain.policy.SanctionPolicy;
import com.feedbacker.report.domain.policy.SanctionPolicy.BanDuration;
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

    private final ReportRepository reportRepository;
    private final ReportSanctionRepository reportSanctionRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final FeedbackPostForceDeleteService feedbackPostForceDeleteService;

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

    @Transactional
    public void sanction(UUID adminId, UUID reportId, ReportSanctionRequest request) {
        Report report = findReport(reportId);
        request.actions().forEach(action -> report.applyAction(action, adminId, request.adminMemo()));

        Member suspect = findSuspect(report);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusYears(SanctionPolicy.COUNT_PERIOD_YEARS);

        // 제제 로직 실행
        // 경고인 경우
        if (request.hasAction(ReportAction.WARNING)) {
            applyWarning(report, suspect, adminId, since);
        }
        // 밴인 경우
        if (request.hasAction(ReportAction.BANNED)) {
            applyPolicyBan(report, suspect, adminId, since, now);
        }
        // 커스텀인 경우
        if (request.hasAction(ReportAction.CUSTOM)) {
            applyCustom(report, suspect, adminId, request, now);
        }
    }

    /** 경고: 최근 1년 경고 횟수에 따라 습도 차등 감소 */
    private void applyWarning(Report report, Member suspect, UUID adminId, LocalDateTime since) {
        int nth = (int) reportSanctionRepository.countWarningsSince(suspect.getId(), since) + 1;
        BigDecimal decreased = suspect.decreaseHumidity(SanctionPolicy.humidityDecreaseFor(nth));
        reportSanctionRepository.save(sanctionOf(report, ReportAction.WARNING, adminId)
                .humidityDecreased(decreased)
                .build());
    }

    /** 밴: 최근 1년 밴 횟수에 따라 정지 기간 차등 적용 */
    private void applyPolicyBan(Report report, Member suspect, UUID adminId,
                                LocalDateTime since, LocalDateTime now) {
        int nth = (int) reportSanctionRepository.countBansSince(suspect.getId(), since) + 1;
        ReportSanction.ReportSanctionBuilder sanction = sanctionOf(report, ReportAction.BANNED, adminId);
        applyBan(suspect, SanctionPolicy.banDurationFor(nth), now, sanction);
        reportSanctionRepository.save(sanction.build());
    }

    /** 커스텀: 입력한 수치 그대로 적용 */
    private void applyCustom(Report report, Member suspect, UUID adminId,
                             ReportSanctionRequest request, LocalDateTime now) {
        ReportSanction.ReportSanctionBuilder sanction = sanctionOf(report, ReportAction.CUSTOM, adminId);
        if (request.humidityDecrease() != null) {
            sanction.humidityDecreased(suspect.decreaseHumidity(request.humidityDecrease()));
        }
        if (request.permanent()) {
            applyBan(suspect, BanDuration.ofPermanent(), now, sanction);
        } else if (request.banDays() != null) {
            applyBan(suspect, BanDuration.ofDays(request.banDays()), now, sanction);
        }
        reportSanctionRepository.save(sanction.build());
    }

    /** 정지를 적용하고 그 결과를 제재 기록에 채운다. 이미 영구 정지 중이면 정지 종료 시각(banEndAt)을 비워 둔다 */
    private void applyBan(Member suspect, BanDuration duration, LocalDateTime now,
                          ReportSanction.ReportSanctionBuilder sanction) {
        LocalDateTime before = suspect.getBannedUntil();
        // applied = 밴을 적용할 것인가 (이미 영구정지 상태면 false)
        boolean applied = duration.permanent()
                ? suspect.banPermanently()
                : suspect.ban(duration.days(), now);
        sanction.banDays(duration.days())
                .permanent(duration.permanent())
                .bannedUntilBefore(before)
                .banEndAt(applied ? suspect.getBannedUntil() : null);
    }

    /**
     * 신고 당한 컨텐츠를 소프트 삭제 (30일 후 물리 삭제).
     * 피드백 모집글은 바로 정산까지 처리한다 (제출된 피드백 보상 지급, 남은 예치 도토리 소멸)
     */
    @Transactional
    public void deleteContent(UUID adminId, UUID reportId, ReportProcessRequest request) {
        Report report = findReport(reportId);
        report.applyAction(ReportAction.CONTENT_DELETED, adminId, memoOf(request));

        Integer acornForfeited = null;
        switch (report.getType()) {
            case PROJECT_POST -> findProject(report).delete();
            case FEEDBACK_POST -> acornForfeited = feedbackPostForceDeleteService.forceDelete(report.getTargetId());
            case MEMBER -> throw new BusinessException(ReportErrorCode.CONTENT_DELETION_NOT_SUPPORTED);
            // TODO: 댓글 기능 추가후 추가
            case COMMENT -> throw new BusinessException(ReportErrorCode.REPORT_TYPE_NOT_SUPPORTED);
        }
        reportSanctionRepository.save(sanctionOf(report, ReportAction.CONTENT_DELETED, adminId)
                .acornForfeited(acornForfeited)
                .build());

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
        boolean banReverted = false;

        for (ReportSanction sanction : sanctions) {
            if (sanction.hasHumidityDecrease()) {
                if (suspect == null) {
                    suspect = findSuspect(report);
                }
                suspect.increaseHumidity(sanction.getHumidityDecreased());
            }
            if (sanction.hasBan()) {
                banReverted = true;
            }
            if (sanction.getAction() == ReportAction.CONTENT_DELETED) {
                restoreContent(report, sanction);
            }
            sanction.revert(adminId, now);
        }

        if (banReverted) {
            if (suspect == null) {
                suspect = findSuspect(report);
            }
            recalculateBan(suspect, now);
        }
    }

    /**
     * 남아 있는(원복되지 않은) 정지 제재만으로 정지 종료 시각을 다시 계산한다.
     * 일수를 빼거나 저장해 둔 이전 값으로 되돌리는 방식은 원복 순서에 따라
     * 이미 원복된 정지가 되살아날 수 있어서 쓰지 않는다
     */
    private void recalculateBan(Member suspect, LocalDateTime now) {
        // 방금 원복 표시한 제재가 아직 flush 전이어도 빠지도록 메모리 상태로 한 번 더 거른다
        List<ReportSanction> activeBans = reportSanctionRepository.findActiveBans(suspect.getId()).stream()
                .filter(sanction -> sanction.getRevertedAt() == null)
                .toList();
        suspect.changeBannedUntil(ReportSanction.calculateBannedUntil(activeBans, now));
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

    private void restoreContent(Report report, ReportSanction sanction) {
        switch (report.getType()) {
            case PROJECT_POST -> findProject(report).restore();
            case FEEDBACK_POST -> feedbackPostForceDeleteService.restore(report.getTargetId(), sanction.getAcornForfeited());
            case COMMENT -> { } // TODO: 신고 당한 사람에게 알림 전송
            case MEMBER -> { } // 멤버는 삭제되지 않음
        }
    }

    private ReportSanction.ReportSanctionBuilder sanctionOf(Report report, ReportAction action, UUID adminId) {
        return ReportSanction.builder()
                .reportId(report.getId())
                .suspectId(report.getSuspectId())
                .action(action)
                .processedBy(adminId);
    }

    private String memoOf(ReportProcessRequest request) {
        return request == null ? null : request.adminMemo();
    }
}
