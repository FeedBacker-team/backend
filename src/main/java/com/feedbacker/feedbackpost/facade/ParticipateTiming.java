package com.feedbacker.feedbackpost.facade;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [부하 테스트용 측정 코드] 참여 요청의 구간별 시간을 측정해 트랜잭션이 끝난 뒤 로그로 남긴다.
 * <p>
 * 트랜잭션 시작(커넥션 획득)은 participate() 본문 밖에서 일어나므로, 요청 도착 시각은 컨트롤러에서 {@link #start()}로 기록한다.
 * 락은 커밋(또는 롤백) 때 풀리므로, 락 보유 시간은 트랜잭션 완료 시점(afterCompletion)까지로 잰다.
 * <ul>
 *     <li>connWait: 요청 도착 → 커넥션 획득(트랜잭션 시작)</li>
 *     <li>lockWait: 슬롯 차감 UPDATE 요청 → 응답(DB 왕복 1회 포함)</li>
 *     <li>lockHold: UPDATE 응답 → 커밋/롤백 완료</li>
 * </ul>
 * 사전 검증에서 거절되면 락을 잡지 않으므로 lock 값은 -1로 남는다.
 */
@Slf4j
public final class ParticipateTiming {

    private static final ThreadLocal<Long> REQUEST_STARTED_AT = new ThreadLocal<>();

    private final long requestStartedAt;
    private final long txStartedAt;
    private long lockRequestedAt = -1;
    private long lockAcquiredAt = -1;

    private ParticipateTiming(long requestStartedAt, long txStartedAt) {
        this.requestStartedAt = requestStartedAt;
        this.txStartedAt = txStartedAt;
    }

    /** 컨트롤러(트랜잭션 밖)에서 요청 도착 시각을 기록한다 */
    public static void start() {
        REQUEST_STARTED_AT.set(System.nanoTime());
    }

    public static void clear() {
        REQUEST_STARTED_AT.remove();
    }

    /** 트랜잭션 안에서 측정을 시작하고, 트랜잭션이 끝나면 로그를 남기도록 등록한다 */
    static ParticipateTiming begin() {
        long now = System.nanoTime();
        Long startedAt = REQUEST_STARTED_AT.get();
        ParticipateTiming timing = new ParticipateTiming(startedAt != null ? startedAt : now, now);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                timing.log(status == STATUS_COMMITTED);
            }
        });
        return timing;
    }

    void lockRequested() {
        lockRequestedAt = System.nanoTime();
    }

    void lockAcquired() {
        lockAcquiredAt = System.nanoTime();
    }

    private void log(boolean committed) {
        long completedAt = System.nanoTime();
        long lockWait = lockRequestedAt < 0 ? -1
                : toMillis((lockAcquiredAt < 0 ? completedAt : lockAcquiredAt) - lockRequestedAt);
        long lockHold = lockAcquiredAt < 0 ? -1 : toMillis(completedAt - lockAcquiredAt);
        log.info("[participate-timing] result={} connWaitMs={} lockWaitMs={} lockHoldMs={} totalMs={}",
                committed ? "COMMITTED" : "ROLLED_BACK",
                toMillis(txStartedAt - requestStartedAt), lockWait, lockHold,
                toMillis(completedAt - requestStartedAt));
    }

    private static long toMillis(long nanos) {
        return nanos / 1_000_000;
    }
}
