package com.feedbacker.feedback.domain.type;

public enum FeedbackStatus {
    WRITING,    // 작성중 (제출 전 상태)
    SUBMITTED,  // 제출됨 (심사 대기 상태)
    ACCEPTED,   // 수락됨
    REJECTED,   // 거절됨
    CANCELED,   // 취소됨 (피드백 포기 상태)
    EXPIRED,    // 만료됨 (제출 기한이 지난 상태)
    OBJECTED,   // 이의제기됨 (어드민 검토 대기 상태)
    OBJECTION_ACCEPTED, // 이의제기 인용됨 (검토 완료, 거절 번복)
    OBJECTION_REJECTED, // 이의제기 기각됨 (검토 완료, 거절 확정)
}
