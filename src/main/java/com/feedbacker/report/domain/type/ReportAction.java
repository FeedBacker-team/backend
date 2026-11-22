package com.feedbacker.report.domain.type;

public enum ReportAction {
    NONE,             // 아무 처리 안 함 (신고가 타당하지 않은 경우)
    WARNING,          // 경고 처리 (습도 감소)
    BANNED,           // 밴 처리 (n일 정지)
    CONTENT_DELETED,  // 신고 당한 컨텐츠 삭제
    CUSTOM,           // 제재 커스텀 (예. 습도 한 번에 50 감소)
    REVERTED;         // 적용했던 제재를 전체 원복함

    /** 실제 제재가 아닌 처리 결과 표시 (기각, 원복) */
    public boolean isResultMarker() {
        return this == NONE || this == REVERTED;
    }
}
