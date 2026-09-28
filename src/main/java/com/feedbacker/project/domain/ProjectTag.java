package com.feedbacker.project.domain;


public enum ProjectTag {

    WEB("웹"),
    APP("앱"),
    AI("AI · ML"),
    DATA("데이터"),
    CLOUD("클라우드"),
    COMMERCE("커머스"),
    FINTECH("핀테크"),
    B2B("B2B · SaaS"),
    CONTENT_MEDIA("콘텐츠 · 미디어"),
    GAME("게임"),
    UX("UXUI"),
    SECURITY("보안"),
    PRODUCTIVITY("생산성"),
    HEALTHCARE("헬스케어"),
    GLOBAL("글로벌");

    private final String displayName;

    ProjectTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
