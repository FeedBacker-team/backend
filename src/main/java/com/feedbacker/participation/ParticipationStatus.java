package com.feedbacker.participation;

public enum ParticipationStatus {
    RESERVED, //슬롯 예약, 피드백 미제출
    SUBMITTED, //피드백 최종 제출
    ABANDONED, //참여자가 피드백 포기
    EXPIRED //제출 기한 초과, 노쇼
}
