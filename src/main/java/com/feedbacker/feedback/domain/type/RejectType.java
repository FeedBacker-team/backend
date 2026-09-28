package com.feedbacker.feedback.domain.type;

public enum RejectType {
    IRRELEVANT_ANSWER,  // 질문과 무관한 답변
    LOW_EFFORT_ANSWER,  // 성의 없이 작성된 답변
    TEST_NOT_PERFORMED, // 테스트를 실제로 진행하지 않음
    OTHER
}
