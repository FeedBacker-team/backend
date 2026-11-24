package com.feedbacker.report;

import com.feedbacker.feedbackpost.service.FeedbackPostDeleteService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.ReportSanction;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.repository.ReportRepository;
import com.feedbacker.report.repository.ReportSanctionRepository;
import com.feedbacker.report.service.AdminReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 정지 원복 시 원복 순서와 상관없이 남은 제재만으로 정지 종료 시각이 정해지는지 */
class BanRevertTest {

    private final ReportRepository reports = mock(ReportRepository.class);
    private final ReportSanctionRepository sanctions = mock(ReportSanctionRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final AdminReportService service = new AdminReportService(reports, sanctions, members,
            mock(ProjectRepository.class), mock(FeedbackPostDeleteService.class));

    private final UUID suspectId = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.createEmailMember("ban@example.com", "password");
        ReflectionTestUtils.setField(member, "id", suspectId);
        when(members.findById(suspectId)).thenReturn(Optional.of(member));
    }

    @Test
    void 기간정지_원복_후_영구정지_원복하면_정지가_되살아나지_않는다() {
        // A: 2일 전 6일 정지 (4일 뒤 종료), B: 1일 전 영구 정지
        ReportSanction a = periodBan(now.minusDays(2), 6);
        ReportSanction b = permanentBan(now.minusDays(1), now.plusDays(4));
        member.changeBannedUntil(Member.PERMANENT_BAN_UNTIL);
        when(sanctions.findActiveBans(suspectId)).thenReturn(List.of(a, b));

        UUID reportA = report(a);
        UUID reportB = report(b);

        service.revert(admin, reportA, null);
        assertThat(member.isPermanentlyBanned()).isTrue();

        service.revert(admin, reportB, null);
        assertThat(member.getBannedUntil()).isNull();
        assertThat(member.isBanned(now)).isFalse();
    }

    @Test
    void 영구정지_원복_후_기간정지_원복해도_결과가_같다() {
        ReportSanction a = periodBan(now.minusDays(2), 6);
        ReportSanction b = permanentBan(now.minusDays(1), now.plusDays(4));
        member.changeBannedUntil(Member.PERMANENT_BAN_UNTIL);
        when(sanctions.findActiveBans(suspectId)).thenReturn(List.of(a, b));

        UUID reportA = report(a);
        UUID reportB = report(b);

        service.revert(admin, reportB, null);
        assertThat(member.getBannedUntil()).isEqualTo(now.plusDays(4));

        service.revert(admin, reportA, null);
        assertThat(member.getBannedUntil()).isNull();
    }

    @Test
    void 이어붙인_기간정지_중_앞의_것을_원복하면_뒤의_정지만_남는다() {
        // A: 2일 전 6일 정지 (4일 뒤 종료), C: 1일 전 3일 정지 → A 뒤에 이어 붙어 7일 뒤 종료
        ReportSanction a = periodBan(now.minusDays(2), 6);
        ReportSanction c = periodBan(now.minusDays(1), 3);
        member.changeBannedUntil(now.plusDays(7));
        when(sanctions.findActiveBans(suspectId)).thenReturn(List.of(a, c));

        service.revert(admin, report(a), null);

        // C 단독이면 1일 전부터 3일 → 2일 뒤 종료
        assertThat(member.getBannedUntil()).isEqualTo(now.plusDays(2));
    }

    @Test
    void 남은_정지가_이미_끝났으면_정지_해제() {
        ReportSanction old = periodBan(now.minusDays(10), 3);
        ReportSanction b = permanentBan(now.minusDays(1), null);
        member.changeBannedUntil(Member.PERMANENT_BAN_UNTIL);
        when(sanctions.findActiveBans(suspectId)).thenReturn(List.of(old, b));

        service.revert(admin, report(b), null);

        assertThat(member.getBannedUntil()).isNull();
    }

    private ReportSanction periodBan(LocalDateTime createdAt, int days) {
        ReportSanction sanction = ReportSanction.builder()
                .suspectId(suspectId)
                .action(ReportAction.CUSTOM)
                .banDays(days)
                .processedBy(admin)
                .build();
        ReflectionTestUtils.setField(sanction, "createdAt", createdAt);
        return sanction;
    }

    private ReportSanction permanentBan(LocalDateTime createdAt, LocalDateTime bannedUntilBefore) {
        ReportSanction sanction = ReportSanction.builder()
                .suspectId(suspectId)
                .action(ReportAction.CUSTOM)
                .permanent(true)
                .bannedUntilBefore(bannedUntilBefore)
                .banEndAt(Member.PERMANENT_BAN_UNTIL)
                .processedBy(admin)
                .build();
        ReflectionTestUtils.setField(sanction, "createdAt", createdAt);
        return sanction;
    }

    /** 이 제재 하나만 가진 신고를 만들고 아이디를 반환 */
    private UUID report(ReportSanction sanction) {
        UUID reportId = UUID.randomUUID();
        Report report = mock(Report.class);
        when(report.getSuspectId()).thenReturn(suspectId);
        when(reports.findById(reportId)).thenReturn(Optional.of(report));
        when(sanctions.findAllByReportIdAndRevertedAtIsNullOrderByCreatedAtDesc(reportId))
                .thenReturn(List.of(sanction));
        return reportId;
    }
}
