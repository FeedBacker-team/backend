package com.feedbacker.report.service;

import com.feedbacker.report.domain.dto.request.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    public void report(UUID reporterId, ReportCreateRequest request) {
        // TODO: 신고 로직 (request.type()에 따라 대상 조회 및 suspectId 결정)
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
