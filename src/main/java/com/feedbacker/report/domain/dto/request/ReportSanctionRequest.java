package com.feedbacker.report.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.report.domain.type.ReportAction;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 제재 요청.
 * - WARNING(습도 고정 감소), BANNED(고정 일수 정지)는 함께 선택 가능, 수치 입력 없음
 * - CUSTOM은 단독으로만 선택 가능 (humidityDecrease, banDays 중 하나 이상 입력)
 */
public record ReportSanctionRequest(

        @NotEmpty(message = "제재 유형을 하나 이상 선택해야 합니다.")
        List<@NotNull ReportAction> actions,

        @Positive(message = "정지 일수는 양수여야 합니다.")
        Integer banDays,

        @Positive(message = "감소할 습도는 양의 정수여야 합니다.")
        @Digits(integer = 3, fraction = 0, message = "감소할 습도는 정수여야 합니다.")
        BigDecimal humidityDecrease,

        @Size(max = 1000, message = "처리 근거는 1000자 이하여야 합니다.")
        String adminMemo
) {

    private static final Set<ReportAction> SANCTION_ACTIONS =
            EnumSet.of(ReportAction.WARNING, ReportAction.BANNED, ReportAction.CUSTOM);

    @AssertTrue(message = "제재 유형은 경고, 밴, 커스텀 중에서 중복 없이 선택해야 합니다.")
    @JsonIgnore
    public boolean isActionsValid() {
        if (actions == null) {
            return true;
        }
        return actions.stream().allMatch(action -> action == null || SANCTION_ACTIONS.contains(action))
                && actions.stream().distinct().count() == actions.size();
    }

    @AssertTrue(message = "커스텀 제재는 다른 제재와 함께 선택할 수 없습니다.")
    @JsonIgnore
    public boolean isCustomAlone() {
        return !isCustom() || actions.size() == 1;
    }

    @AssertTrue(message = "커스텀 제재는 습도 감소 또는 정지 일수 중 하나 이상을 입력해야 합니다.")
    @JsonIgnore
    public boolean isCustomValuePresent() {
        return !isCustom() || humidityDecrease != null || banDays != null;
    }

    @AssertTrue(message = "습도 감소량은 커스텀 제재에서만 입력할 수 있습니다.")
    @JsonIgnore
    public boolean isHumidityDecreaseOnlyForCustom() {
        return humidityDecrease == null || isCustom();
    }

    @AssertTrue(message = "정지 일수는 커스텀 제재에서만 입력할 수 있습니다.")
    @JsonIgnore
    public boolean isBanDaysOnlyForCustom() {
        return banDays == null || isCustom();
    }

    public boolean hasAction(ReportAction action) {
        return actions != null && actions.contains(action);
    }

    private boolean isCustom() {
        return hasAction(ReportAction.CUSTOM);
    }
}
