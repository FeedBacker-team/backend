package com.feedbacker.report.repository;

import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.type.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterIdAndTypeAndTargetId(UUID reporterId, ReportType type, UUID targetId);

    Page<Report> findAllByProcessed(boolean processed, Pageable pageable);

    /** threshold 이전에 생성된 유효한 신고를 모두 무효 처리하고, 변경된 건수를 반환 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Report r
            set r.valid = false
            where r.valid = true
              and r.createdAt <= :threshold
            """)
    int invalidateCreatedBefore(@Param("threshold") LocalDateTime threshold);
}
