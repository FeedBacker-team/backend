package com.feedbacker.report.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.ProjectStatus;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.dto.request.ReportCreateRequest;
import com.feedbacker.report.exception.ReportErrorCode;
import com.feedbacker.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    // 신고가 제재 판단에 유효한 기간
    private static final Period VALID_PERIOD = Period.ofYears(1);

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final FeedbackPostRepository feedbackPostRepository;

    @Transactional
    public void report(UUID reporterId, ReportCreateRequest request) {
        UUID suspectId = resolveSuspectId(request);

        if (reporterId.equals(suspectId)) {
            throw new BusinessException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
        if (reportRepository.existsByReporterIdAndTypeAndTargetId(reporterId, request.type(), request.targetId())) {
            throw new BusinessException(ReportErrorCode.ALREADY_REPORTED);
        }

        reportRepository.save(Report.builder()
                .reporterId(reporterId)
                .suspectId(suspectId)
                .type(request.type())
                .targetId(request.targetId())
                .reasons(request.reasons())
                .detailReasons(request.detailReasons())
                .build());
    }

    /** 생성된 지 VALID_PERIOD가 지난 신고를 무효 처리 (스케줄러 전용) */
    @Transactional
    public int invalidateExpiredReports(LocalDateTime now) {
        return reportRepository.invalidateCreatedBefore(now.minus(VALID_PERIOD));
    }

    /** 신고 대상이 존재하는지 확인하고, 신고 당한 회원 아이디를 반환 */
    private UUID resolveSuspectId(ReportCreateRequest request) {
        UUID targetId = request.targetId();
        return switch (request.type()) {
            case MEMBER -> memberRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND))
                    .getId();
            case PROJECT_POST -> projectRepository.findByIdAndStatus(targetId, ProjectStatus.PUBLISHED)
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND))
                    .getOwner().getId();
            case FEEDBACK_POST -> feedbackPostRepository.findById(targetId)
                    .filter(feedbackPost -> !feedbackPost.isDeleted())
                    .map(FeedbackPost::getWriterId)
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
            // TODO: Comment 엔티티가 생기면 작성자 조회로 교체
            case COMMENT -> throw new BusinessException(ReportErrorCode.REPORT_TYPE_NOT_SUPPORTED);
        };
    }
}
