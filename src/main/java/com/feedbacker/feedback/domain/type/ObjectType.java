package com.feedbacker.feedback.domain.type;

public enum ObjectType {
    FAITHFUL_ANSWER,        // 요청사항에 맞게 성실하게 작성함
    REJECT_REASON_MISMATCH, // 거절 사유가 실제 제출한 피드백 내용과 다름
    QA_PROJECT_ISSUE,       // QA 프로젝트 문제(오류·자료 부족)로 진행에 한계가 있었음
    OTHER
}
