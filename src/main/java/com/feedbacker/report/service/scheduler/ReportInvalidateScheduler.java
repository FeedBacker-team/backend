package com.feedbacker.report.service.scheduler;

import com.feedbacker.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportInvalidateScheduler {

    private final ReportService reportService;

    // 매일 04:30, 생성된 지 1년이 지난 신고를 무효 처리 (제재 시 유효한 신고 개수에서 제외)
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void invalidateExpiredReports() {
        try {
            int invalidatedCount = reportService.invalidateExpiredReports(LocalDateTime.now());
            if (invalidatedCount > 0) {
                log.info("기간 만료 신고 무효 처리: {}건", invalidatedCount);
            }
        } catch (Exception e) {
            log.error("기간 만료 신고 무효 처리 실패", e);
        }
    }
}
