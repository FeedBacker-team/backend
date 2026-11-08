package com.feedbacker.report.domain;

import com.feedbacker.global.common.BaseTimeEntity;
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
}
