package com.feedbacker.feedbackpost.facade;

import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
public class ParticipateBulkhead {

    private final Semaphore permits;

    public ParticipateBulkhead(@Value("${feedbacker.participate.max-concurrent:5}") int maxConcurrent) {
        this.permits = new Semaphore(maxConcurrent);
    }

    public void run(Runnable task) {
        if (!permits.tryAcquire()) {
            throw new BusinessException(FeedbackPostErrorCode.PARTICIPATION_BUSY);
        }
        try {
            task.run();
        } finally {
            permits.release();
        }
    }
}
