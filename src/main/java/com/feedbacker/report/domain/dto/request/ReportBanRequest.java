package com.feedbacker.report.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 밴 처리 요청 */
public record ReportBanRequest(

        @NotNull(message = "정지 일수는 필수입니다.")
        @Positive(message = "정지 일수는 양수여야 합니다.")
        Integer days,

        @Size(max = 1000, message = "처리 근거는 1000자 이하여야 합니다.")
        String adminMemo
) {
}
