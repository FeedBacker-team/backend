package com.feedbacker.report.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.member.Member;
import com.feedbacker.report.domain.type.ReportAction;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 신고 처리 시 실제로 적용된 제재 기록 (원복할 때 이 기록만큼 되돌림, 차등 제재 횟수 계산에도 사용) */
@Entity
@Table(name = "report_sanctions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_sanction_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // 제재 대상 신고 (DB FK: report 삭제 시 CASCADE로 ReportSanction도 삭제)
    @Column(nullable = false)
    private UUID reportId;

    // 제재 받은 회원 (경고·밴 횟수 계산용)
    @Column(nullable = false)
    private UUID suspectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportAction action;

    // 실제로 감소한 습도 (0 하한 때문에 요청값보다 작을 수 있음). null이 아니면 경고 1회로 센다
    @Column(precision = 5, scale = 2)
    private BigDecimal humidityDecreased;

    // 부여한 정지 일수. null이 아니면 밴 1회로 센다
    @Column
    private Integer banDays;

    // 영구 정지 여부. true면 밴 1회로 센다
    @Column(nullable = false)
    private boolean permanent;

    // 이 제재 적용 직전의 정지 종료 시각 (기록용. 원복은 calculateBannedUntil로 다시 계산한다)
    @Column
    private LocalDateTime bannedUntilBefore;

    // 이 제재 적용 직후의 정지 종료 시각. 이미 영구 정지 중이라 정지가 실제로 적용되지 않았으면 null
    @Column
    private LocalDateTime banEndAt;

    // 피드백 모집글 강제 삭제 시 소멸시킨 예치 도토리. 삭제 전에 이미 정산(완료)된 모집글이면 null
    @Column
    private Integer acornForfeited;

    @Column(nullable = false)
    private UUID processedBy;

    @Column
    private LocalDateTime revertedAt;

    @Column
    private UUID revertedBy;

    @Builder
    public ReportSanction(
            UUID reportId,
            UUID suspectId,
            ReportAction action,
            BigDecimal humidityDecreased,
            Integer banDays,
            boolean permanent,
            LocalDateTime bannedUntilBefore,
            LocalDateTime banEndAt,
            Integer acornForfeited,
            UUID processedBy
    ) {
        this.reportId = reportId;
        this.suspectId = suspectId;
        this.action = action;
        this.humidityDecreased = humidityDecreased;
        this.banDays = banDays;
        this.permanent = permanent;
        this.bannedUntilBefore = bannedUntilBefore;
        this.banEndAt = banEndAt;
        this.acornForfeited = acornForfeited;
        this.processedBy = processedBy;
    }

    public boolean hasHumidityDecrease() {
        return humidityDecreased != null && humidityDecreased.signum() > 0;
    }

    public boolean hasBan() {
        return banDays != null || permanent;
    }

    /**
     * 원복되지 않은 정지 제재들로 회원의 정지 종료 시각을 다시 계산한다.
     * - 하나라도 영구 정지면 영구 정지
     * - 기간 정지는 생성 순서대로, 앞선 정지가 끝나지 않았으면 그 뒤에 이어 붙인다 (ban()과 같은 규칙)
     * - 계산 결과가 now 이전이면 정지 해제(null)
     * 원복 순서와 상관없이 남은 제재만으로 결과가 정해진다.
     *
     * @param sanctions 원복되지 않은 정지 제재 (생성 시각 오름차순)
     */
    public static LocalDateTime calculateBannedUntil(List<ReportSanction> sanctions, LocalDateTime now) {
        LocalDateTime end = null;
        for (ReportSanction sanction : sanctions) {
            if (sanction.isPermanent()) {
                return Member.PERMANENT_BAN_UNTIL;
            }
            if (sanction.getBanDays() == null) {
                continue;
            }
            LocalDateTime start = sanction.getCreatedAt();
            LocalDateTime base = end != null && end.isAfter(start) ? end : start;
            end = base.plusDays(sanction.getBanDays());
        }
        return end != null && end.isAfter(now) ? end : null;
    }

    public void revert(UUID adminId, LocalDateTime now) {
        this.revertedAt = now;
        this.revertedBy = adminId;
    }
}
