package com.feedbacker.report;

import com.feedbacker.feedbackpost.service.FeedbackPostDeleteService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.report.domain.Report;
import com.feedbacker.report.domain.ReportSanction;
import com.feedbacker.report.domain.dto.request.ReportSanctionRequest;
import com.feedbacker.report.domain.type.ReportAction;
import com.feedbacker.report.repository.ReportRepository;
import com.feedbacker.report.repository.ReportSanctionRepository;
import com.feedbacker.report.service.AdminReportService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomPermanentSanctionTest {
    @Test
    void validatesCustomPermanentAndExistingRequests() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(request(ReportAction.CUSTOM, null, true, null))).isEmpty();
            assertThat(validator.validate(request(ReportAction.CUSTOM, null, true, BigDecimal.TEN))).isEmpty();
            assertThat(validator.validate(request(ReportAction.CUSTOM, 7, false, null))).isEmpty();
            assertThat(validator.validate(request(ReportAction.BANNED, null, false, null))).isEmpty();
            assertThat(validator.validate(request(ReportAction.CUSTOM, 7, true, null))).isNotEmpty();
            assertThat(validator.validate(request(ReportAction.BANNED, null, true, null))).isNotEmpty();
            assertThat(validator.validate(request(ReportAction.WARNING, null, true, null))).isNotEmpty();
            assertThat(validator.validate(request(ReportAction.CUSTOM, null, false, null))).isNotEmpty();
        }
    }

    @Test
    void customPermanentBansMemberAndRecordsRevertState() {
        var reports = mock(ReportRepository.class);
        var sanctions = mock(ReportSanctionRepository.class);
        var members = mock(MemberRepository.class);
        var service = new AdminReportService(reports, sanctions, members,
                mock(ProjectRepository.class), mock(FeedbackPostDeleteService.class));
        UUID reportId = UUID.randomUUID();
        UUID suspectId = UUID.randomUUID();
        var report = mock(Report.class);
        var member = Member.createEmailMember("custom@example.com", "password");
        member.ban(7, java.time.LocalDateTime.now());
        var before = member.getBannedUntil();
        when(reports.findById(reportId)).thenReturn(Optional.of(report));
        when(report.getSuspectId()).thenReturn(suspectId);
        when(members.findById(suspectId)).thenReturn(Optional.of(member));

        service.sanction(UUID.randomUUID(), reportId, request(ReportAction.CUSTOM, null, true, null));

        assertThat(member.isPermanentlyBanned()).isTrue();
        var captor = ArgumentCaptor.forClass(ReportSanction.class);
        verify(sanctions).save(captor.capture());
        var sanction = captor.getValue();
        assertThat(sanction.isPermanent()).isTrue();
        assertThat(sanction.getBanDays()).isNull();
        assertThat(sanction.getBannedUntilBefore()).isEqualTo(before);
        assertThat(sanction.getBanEndAt()).isEqualTo(Member.PERMANENT_BAN_UNTIL);
    }

    private ReportSanctionRequest request(ReportAction action, Integer days, boolean permanent, BigDecimal humidity) {
        return new ReportSanctionRequest(List.of(action), days, permanent, humidity, null);
    }
}
