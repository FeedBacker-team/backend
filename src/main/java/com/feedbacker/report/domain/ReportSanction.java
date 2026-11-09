package com.feedbacker.report.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.report.domain.type.ReportAction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** 신고 처리 시 실제로 적용된 제재 기록 (원복할 때 이 기록만큼 되돌림) */
@Entity
@Table(name = "report_sanctions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_sanction_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportAction action;

    // 실제로 감소한 습도 (0 하한 때문에 요청값보다 작을 수 있음)
    @Column(precision = 5, scale = 2)
    private BigDecimal humidityDecreased;

    // 부여한 정지 일수
    @Column
    private Integer banDays;

    // 이 제재 적용 직후의 정지 종료 시각
    @Column
    private LocalDateTime banEndAt;

    @Column(nullable = false)
    private UUID processedBy;

    @Column
    private LocalDateTime revertedAt;

    @Column
    private UUID revertedBy;

    @Builder
    public ReportSanction(
            UUID reportId,
            ReportAction action,
            BigDecimal humidityDecreased,
            Integer banDays,
            LocalDateTime banEndAt,
            UUID processedBy
    ) {
        this.reportId = reportId;
        this.action = action;
        this.humidityDecreased = humidityDecreased;
        this.banDays = banDays;
        this.banEndAt = banEndAt;
        this.processedBy = processedBy;
    }

    public boolean hasHumidityDecrease() {
        return humidityDecreased != null && humidityDecreased.signum() > 0;
    }

    public boolean hasBan() {
        return banDays != null;
    }

    public void revert(UUID adminId, LocalDateTime now) {
        this.revertedAt = now;
        this.revertedBy = adminId;
    }
}
