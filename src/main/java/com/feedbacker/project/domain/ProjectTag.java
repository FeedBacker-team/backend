package com.feedbacker.project.domain;


public enum ProjectTag {

    WEB("웹"),
    APP("앱"),
    AI("AI"),
    UX("UX"),
    B2B("B2B"),
    COMMERCE("커머스"),
    GAME("게임"),
    DATA("데이터");

    private final String displayName;

    ProjectTag(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
