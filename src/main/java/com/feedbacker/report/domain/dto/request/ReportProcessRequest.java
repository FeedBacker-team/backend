package com.feedbacker.report.domain.dto.request;

import jakarta.validation.constraints.Size;

/** 경고, 컨텐츠 삭제, 복구, 기각 처리 요청 (body 생략 가능) */
public record ReportProcessRequest(

        @Size(max = 1000, message = "처리 근거는 1000자 이하여야 합니다.")
        String adminMemo
) {
}
