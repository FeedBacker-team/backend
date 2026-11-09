package com.feedbacker.report.repository;

import com.feedbacker.report.domain.ReportSanction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportSanctionRepository extends JpaRepository<ReportSanction, UUID> {

    List<ReportSanction> findAllByReportIdAndRevertedAtIsNullOrderByCreatedAtDesc(UUID reportId);
}
