package com.feedbacker.report.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 커스텀 제재 요청 (습도 감소, 정지 중 하나 이상) */
public record ReportSanctionRequest(

        @Positive(message = "감소할 습도는 양수여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "감소할 습도는 소수점 둘째 자리까지 입력할 수 있습니다.")
        BigDecimal humidityDecrease,

        @Positive(message = "정지 일수는 양수여야 합니다.")
        Integer banDays,

        @Size(max = 1000, message = "처리 근거는 1000자 이하여야 합니다.")
        String adminMemo
) {

    @AssertTrue(message = "습도 감소 또는 정지 일수 중 하나 이상을 입력해야 합니다.")
    @JsonIgnore
    public boolean isAnySanctionPresent() {
        return humidityDecrease != null || banDays != null;
    }
}
