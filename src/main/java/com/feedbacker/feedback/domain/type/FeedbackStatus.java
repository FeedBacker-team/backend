package com.feedbacker.feedback.domain.type;

public enum FeedbackStatus {
    WRITING,    // 작성중 (제출 전 상태)
    SUBMITTED,  // 제출됨 (심사 대기 상태)
    ACCEPTED,   // 수락됨
    REJECTED    // 거절됨
}
