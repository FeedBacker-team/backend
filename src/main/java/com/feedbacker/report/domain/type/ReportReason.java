package com.feedbacker.report.domain.type;

public enum ReportReason {
    ABUSIVE_LANGUAGE,        // 욕설·비하·혐오 표현
    HARASSMENT,              // 괴롭힘·스토킹·특정인 공격
    SPAM,                    // 스팸·광고·도배·홍보
    IMPERSONATION,           // 타인 사칭
    SEXUAL_CONTENT,          // 음란·선정적 내용
    FEEDBACK_ABUSE,          // 피드백 참여 악용
    FRAUD,                   // 사기·금전 요구·허위 정보
    PRIVACY_VIOLATION,       // 개인정보 노출
    COPYRIGHT_INFRINGEMENT,  // 저작권 등 권리 침해
    OFF_TOPIC,               // 서비스 목적·게시글과 무관
    MALICIOUS_CRITICISM,     // 건설적이지 않은 악의적 비난
    OTHER                    // 기타 (직접 입력)
}
