package com.feedbacker.report.repository;

import com.feedbacker.report.domain.ReportSanction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReportSanctionRepository extends JpaRepository<ReportSanction, UUID> {

    List<ReportSanction> findAllByReportIdAndRevertedAtIsNullOrderByCreatedAtDesc(UUID reportId);

    /** 회원이 받은 원복되지 않은 정지 제재 (정지 종료 시각 재계산용, 생성 순) */
    @Query("""
            select s from ReportSanction s
            where s.suspectId = :suspectId
              and s.revertedAt is null
              and (s.banDays is not null or s.permanent = true)
            order by s.createdAt asc
            """)
    List<ReportSanction> findActiveBans(@Param("suspectId") UUID suspectId);

    /** since 이후 받은 경고 횟수 (원복 제외). 습도 감소가 들어간 제재(경고, 커스텀)를 센다 */
    @Query("""
            select count(s) from ReportSanction s
            where s.suspectId = :suspectId
              and s.revertedAt is null
              and s.createdAt >= :since
              and s.humidityDecreased is not null
            """)
    long countWarningsSince(@Param("suspectId") UUID suspectId, @Param("since") LocalDateTime since);

    /** since 이후 받은 밴 횟수 (원복 제외). 정지가 들어간 제재(밴, 커스텀, 영구 정지)를 센다 */
    @Query("""
            select count(s) from ReportSanction s
            where s.suspectId = :suspectId
              and s.revertedAt is null
              and s.createdAt >= :since
              and (s.banDays is not null or s.permanent = true)
            """)
    long countBansSince(@Param("suspectId") UUID suspectId, @Param("since") LocalDateTime since);
}
