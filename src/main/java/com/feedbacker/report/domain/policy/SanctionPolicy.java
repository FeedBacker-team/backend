package com.feedbacker.report.domain.policy;

import java.math.BigDecimal;
import java.util.List;

/**
 * 최근 1년 동안 받은 경고·밴 횟수에 따른 차등 제재 기준.
 * 회차(nth)는 이번 제재를 포함한 횟수 (처음 받는 경고면 1)
 */
public final class SanctionPolicy {

    // 횟수를 세는 기간 (1년)
    public static final int COUNT_PERIOD_YEARS = 1;

    // 경고 1~5회차 습도 감소량(%p), 6회 이상은 100 (습도 0)
    private static final List<BigDecimal> WARNING_HUMIDITY_DECREASES = List.of(
            new BigDecimal("3"),
            new BigDecimal("5"),
            new BigDecimal("10"),
            new BigDecimal("15"),
            new BigDecimal("20")
    );
    private static final BigDecimal MAX_HUMIDITY_DECREASE = new BigDecimal("100");

    // 밴 1~5회차 정지 일수, 6회 이상은 영구 정지
    private static final List<Integer> BAN_DAYS = List.of(3, 7, 14, 30, 365);

    private SanctionPolicy() {
    }

    /** nth번째 경고의 습도 감소량(%p) */
    public static BigDecimal humidityDecreaseFor(int nth) {
        validateNth(nth);
        return nth <= WARNING_HUMIDITY_DECREASES.size()
                ? WARNING_HUMIDITY_DECREASES.get(nth - 1)
                : MAX_HUMIDITY_DECREASE;
    }

    /** nth번째 밴의 정지 기간 */
    public static BanDuration banDurationFor(int nth) {
        validateNth(nth);
        return nth <= BAN_DAYS.size()
                ? BanDuration.ofDays(BAN_DAYS.get(nth - 1))
                : BanDuration.ofPermanent();
    }

    private static void validateNth(int nth) {
        if (nth < 1) {
            throw new IllegalArgumentException("제재 회차는 1 이상이어야 합니다.");
        }
    }

    /** 정지 기간 (영구 정지면 days는 null) */
    public record BanDuration(Integer days, boolean permanent) {

        public static BanDuration ofDays(int days) {
            return new BanDuration(days, false);
        }

        public static BanDuration ofPermanent() {
            return new BanDuration(null, true);
        }
    }
}
