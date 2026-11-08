package com.feedbacker.report.service;

import com.feedbacker.report.domain.dto.response.ReportDetailResponse;
import com.feedbacker.report.domain.dto.response.ReportListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    public ReportListResponse getReports(Boolean isProcessed, int page, int size) {
        // TODO: 신고 목록 조회 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ReportDetailResponse getReport(UUID reportId) {
        // TODO: 신고 상세 조회 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void warn(UUID adminId, UUID reportId) {
        // TODO: 경고 처리 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void ban(UUID adminId, UUID reportId) {
        // TODO: 밴 처리 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void sanction(UUID adminId, UUID reportId) {
        // TODO: 커스텀 제재 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void deleteContent(UUID adminId, UUID reportId) {
        // TODO: 컨텐츠 삭제 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void restoreContent(UUID adminId, UUID reportId) {
        // TODO: 삭제 원상 복구 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void reject(UUID adminId, UUID reportId) {
        // TODO: 기각 처리 로직
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
