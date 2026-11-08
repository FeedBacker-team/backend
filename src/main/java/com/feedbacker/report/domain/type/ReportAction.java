package com.feedbacker.report.domain.type;

public enum ReportAction {
    NONE,             // 아무 처리 안 함 (신고가 타당하지 않은 경우)
    WARNING,          // 경고 처리 (습도 감소)
    BANNED,           // 밴 처리 (n일 정지)
    CONTENT_DELETED,  // 신고 당한 컨텐츠 삭제
    CUSTOM            // 제재 커스텀 (예. 습도 한 번에 50 감소)
}
