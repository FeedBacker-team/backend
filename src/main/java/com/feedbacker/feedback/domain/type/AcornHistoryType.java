package com.feedbacker.feedback.domain.type;

public enum AcornHistoryType {
    FEEDBACK_ACCEPT,
    FEEDBACK_DEPOSIT,   // 모집글 등록 시 예치 (차감)
    FEEDBACK_REFUND,    // 모집글 완료 시 미사용 예치금 환급 (적립)
    SIGNUP_REWARD,
    EVENT_REWARD
}
