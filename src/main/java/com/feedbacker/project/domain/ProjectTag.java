package com.feedbacker.project.domain;


public enum ProjectTag {

    PRODUCTIVITY("생산성"),
    COMMERCE("커머스"),
    FINTECH("금융 · 핀테크"),
    EDUCATION("교육"),
    HEALTHCARE("헬스케어"),
    CONTENT_MEDIA("콘텐츠 · 미디어"),
    LIFESTYLE("라이프스타일"),
    SOCIAL_COMMUNITY("소셜 · 커뮤니티"),
    PETS("반려동물"),
    CAREER("채용 · 커리어"),
    TRAVEL("여행 · 관광"),
    MOBILITY("모빌리티 · 교통"),
    FOOD("음식 · 식품"),
    MARKETING("마케팅 · 광고"),
    GAME("게임"),
    SPORTS("스포츠"),
    REAL_ESTATE("부동산 · 주거");

    private final String displayName;

    ProjectTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
