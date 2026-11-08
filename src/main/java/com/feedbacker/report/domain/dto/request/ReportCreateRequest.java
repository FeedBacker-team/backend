package com.feedbacker.report.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.report.domain.type.ReportReason;
import com.feedbacker.report.domain.type.ReportType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** 신고 요청 (회원, 프로젝트 게시글, 피드백 모집글, 댓글 공통) */
public record ReportCreateRequest(

        @NotNull(message = "신고 유형은 필수입니다.")
        ReportType type,

        @NotNull(message = "신고 대상 아이디는 필수입니다.")
        UUID targetId,

        @NotEmpty(message = "신고 사유는 최소 1개 이상 선택해야 합니다.")
        List<@NotNull ReportReason> reasons,

        List<@Size(max = 1000, message = "상세 사유는 1000자 이하여야 합니다.") String> detailReasons
) {

    @AssertTrue(message = "신고 유형에서 선택할 수 없는 사유가 포함되어 있습니다.")
    @JsonIgnore
    public boolean isReasonsAllowed() {
        if (type == null || reasons == null) {
            return true;
        }
        return reasons.stream().allMatch(reason -> reason == null || type.allows(reason));
    }
}
