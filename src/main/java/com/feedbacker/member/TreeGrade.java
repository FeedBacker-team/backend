package com.feedbacker.member;

import java.math.BigDecimal;

/** 나무 등급 (습도 구간으로 결정) */
public enum TreeGrade {
    DRY("바짝 마른 나무"),          // 20% 미만
    NORMAL("보통 나무"),            // 20% 이상 45% 미만
    FRUITFUL("도토리가 열린 나무"),  // 45% 이상 60% 미만
    LUSH("풍성한 도토리 나무");      // 60% 이상

    private static final BigDecimal NORMAL_MIN = new BigDecimal("20");
    private static final BigDecimal FRUITFUL_MIN = new BigDecimal("45");
    private static final BigDecimal LUSH_MIN = new BigDecimal("60");

    private final String displayName;

    TreeGrade(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TreeGrade fromHumidity(BigDecimal humidity) {
        if (humidity.compareTo(NORMAL_MIN) < 0) return DRY;
        if (humidity.compareTo(FRUITFUL_MIN) < 0) return NORMAL;
        if (humidity.compareTo(LUSH_MIN) < 0) return FRUITFUL;
        return LUSH;
    }
}
