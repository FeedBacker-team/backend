package com.feedbacker.feedbackpost.facade;

/**
 * [부하 테스트용 측정 코드] 참여 요청의 시작 시각을 트랜잭션 밖(컨트롤러)에서 기록한다.
 * 트랜잭션 시작(커넥션 획득)은 participate() 본문 밖에서 일어나므로, 커넥션 대기 시간을 재려면 시작 시각이 필요하다.
 */
public final class ParticipateTiming {

    private static final ThreadLocal<Long> START = new ThreadLocal<>();

    private ParticipateTiming() {
    }

    public static void start() {
        START.set(System.nanoTime());
    }

    public static void clear() {
        START.remove();
    }

    static Long startedAt() {
        return START.get();
    }
}
