package com.feedbacker.report.repository;

import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.type.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTypeAndTargetId(UUID reporterId, ReportType type, UUID targetId);

    Page<Report> findAllByProcessed(boolean processed, Pageable pageable);
}
