package com.feedbacker.report.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.report.exception.ReportErrorCode;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.domain.type.ReportReason;
import com.feedbacker.report.domain.type.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // 신고한 사람의 아이디
    @Column(nullable = false)
    private UUID reporterId;

    // 신고 당한 사람의 아이디
    @Column(nullable = false)
    private UUID suspectId;

    // 신고 당한 컨텐츠의 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType type;

    // 신고 당한 컨텐츠의 고유 아이디
    @Column(nullable = false)
    private UUID targetId;

    // 신고할 때 선택하는 신고 사유
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons", nullable = false, columnDefinition = "jsonb")
    private List<ReportReason> reasons = new ArrayList<>();

    // 신고할 때 적는 상세 사유
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_reasons", columnDefinition = "jsonb")
    private List<String> detailReasons = new ArrayList<>();

    // 관리자가 신고를 처리했는지 여부
    @Column(name = "is_processed", nullable = false)
    private boolean processed = false;

    // 관리자가 신고를 어떻게 처리했는지
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions", columnDefinition = "jsonb")
    private List<ReportAction> actions = new ArrayList<>();

    // 신고를 처리한 관리자의 아이디 (기록용)
    @Column
    private UUID processedBy;

    // 처리 근거 (기록용, 선택)
    @Column(length = 1000)
    private String adminMemo;

    // 관리자가 처리한 시각
    @Column
    private LocalDateTime processedAt;

    // 신고 생성 후 1년이 지나면 false (제재 시 유효한 신고 개수 측정용)
    @Column(name = "is_valid", nullable = false)
    private boolean valid = true;

    @Builder
    public Report(
            UUID reporterId,
            UUID suspectId,
            ReportType type,
            UUID targetId,
            List<ReportReason> reasons,
            List<String> detailReasons
    ) {
        this.reporterId = reporterId;
        this.suspectId = suspectId;
        this.type = type;
        this.targetId = targetId;
        this.reasons = reasons == null ? new ArrayList<>() : new ArrayList<>(reasons);
        this.detailReasons = detailReasons == null ? new ArrayList<>() : new ArrayList<>(detailReasons);
    }

    /** 제재 조치 적용 (기각 상태였다면 기각을 취소하고 적용) */
    public void applyAction(ReportAction action, UUID adminId, String adminMemo) {
        if (action == null || action == ReportAction.NONE) {
            throw new BusinessException(ReportErrorCode.INVALID_REPORT_ACTION);
        }
        if (this.actions.contains(action)) {
            throw new BusinessException(ReportErrorCode.ACTION_ALREADY_APPLIED);
        }
        List<ReportAction> next = new ArrayList<>(this.actions);
        next.remove(ReportAction.NONE);
        next.add(action);
        this.actions = next;
        markProcessed(adminId, adminMemo);
    }

    /** 기각 (아무 조치 없이 처리 완료) */
    public void reject(UUID adminId, String adminMemo) {
        if (hasSanction()) {
            throw new BusinessException(ReportErrorCode.REPORT_ALREADY_SANCTIONED);
        }
        this.actions = new ArrayList<>(List.of(ReportAction.NONE));
        markProcessed(adminId, adminMemo);
    }

    /** 컨텐츠 삭제 조치 취소 */
    public void restoreContent(UUID adminId, String adminMemo) {
        if (!this.actions.contains(ReportAction.CONTENT_DELETED)) {
            throw new BusinessException(ReportErrorCode.CONTENT_NOT_DELETED);
        }
        List<ReportAction> next = new ArrayList<>(this.actions);
        next.remove(ReportAction.CONTENT_DELETED);
        if (next.isEmpty()) {
            next.add(ReportAction.NONE);
        }
        this.actions = next;
        markProcessed(adminId, adminMemo);
    }

    private boolean hasSanction() {
        return this.actions.stream().anyMatch(action -> action != ReportAction.NONE);
    }

    private void markProcessed(UUID adminId, String adminMemo) {
        this.processed = true;
        this.processedBy = adminId;
        this.processedAt = LocalDateTime.now();
        if (adminMemo != null && !adminMemo.isBlank()) {
            this.adminMemo = adminMemo;
        }
    }
}
