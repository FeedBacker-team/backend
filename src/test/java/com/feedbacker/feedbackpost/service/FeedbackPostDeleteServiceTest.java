package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.ObjectType;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedback.service.FeedbackRewardService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.storage.SupabaseStorageService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.feedbacker.project.domain.ProjectTag.WEB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback-post-delete;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("작성자 삭제 시 모집글 정산 후 물리 삭제")
class FeedbackPostDeleteServiceTest {

    private static final int REWARD = 10;

    @Autowired FeedbackPostRepository feedbackPostRepository;
    @Autowired FeedbackRepository feedbackRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired EntityManager entityManager;

    private FeedbackRewardService feedbackRewardService;
    private FeedbackPostDeleteService deleteService;
    private Member writer;
    private Project project;

    @BeforeEach
    void setUp() {
        feedbackRewardService = mock(FeedbackRewardService.class);
        deleteService = new FeedbackPostDeleteService(
                mock(FeedbackPostService.class),
                feedbackPostRepository,
                new FeedbackService(feedbackRepository),
                feedbackRewardService,
                mock(FeedbackPostCompleteService.class),
                mock(SupabaseStorageService.class),
                entityManager
        );
        writer = memberRepository.save(Member.createEmailMember("writer@example.com", "test-password"));
        project = projectRepository.save(Project.create(
                writer, "프로젝트", "설명", List.of(WEB),
                "https://example.com", "https://example.com/thumbnail.png"));
    }

    @Test
    @DisplayName("미처리 피드백(작성중 포함)을 승인하고 이의제기를 인용한 뒤 모집글을 지우며, 피드백 기록은 남는다")
    void deleteAllByWriter() {
        FeedbackPost recruiting = savePost("모집중");
        FeedbackPost withObjection = savePost("이의제기");
        UUID submittedId = saveFeedback(recruiting, "submitted@example.com", FeedbackStatus.SUBMITTED);
        UUID writingId = saveFeedback(recruiting, "writing@example.com", FeedbackStatus.WRITING);
        UUID objectedId = saveFeedback(withObjection, "objected@example.com", FeedbackStatus.OBJECTED);
        entityManager.flush();
        entityManager.clear();

        deleteService.deleteAllByWriter(writer.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(feedbackPostRepository.findAllById(List.of(recruiting.getId(), withObjection.getId()))).isEmpty();
        Feedback submitted = feedbackRepository.findById(submittedId).orElseThrow();
        Feedback writing = feedbackRepository.findById(writingId).orElseThrow();
        Feedback objected = feedbackRepository.findById(objectedId).orElseThrow();
        assertThat(submitted.getStatus()).isEqualTo(FeedbackStatus.ACCEPTED);
        assertThat(submitted.getRewardAcorn()).isEqualTo(REWARD);
        assertThat(writing.getStatus()).isEqualTo(FeedbackStatus.ACCEPT_BY_WRITER_WITHDRAWAL);
        assertThat(writing.getRewardAcorn()).isEqualTo(REWARD);
        assertThat(objected.getStatus()).isEqualTo(FeedbackStatus.OBJECTION_ACCEPTED);
        assertThat(objected.getRewardAcorn()).isEqualTo(REWARD);
        assertThat(List.of(submitted, writing, objected)).allMatch(feedback -> !feedback.hasFeedbackPost());
        verify(feedbackRewardService, times(3)).payReward(any(), any());
    }

    @Test
    @DisplayName("보관 기간이 지나지 않은 모집글은 purge하지 않는다")
    void purgeSkipsNotDeletedPost() {
        FeedbackPost post = savePost("삭제 안 됨");
        entityManager.flush();
        entityManager.clear();

        assertThat(deleteService.purge(post.getId(), LocalDateTime.now())).isFalse();
        assertThat(feedbackPostRepository.findById(post.getId())).isPresent();
    }

    private FeedbackPost savePost(String title) {
        LocalDateTime now = LocalDateTime.now();
        return feedbackPostRepository.save(FeedbackPost.builder()
                .writerId(writer.getId())
                .writerName("작성자")
                .title(title)
                .targetType(TargetType.SERVICE_LINK)
                .serviceUrl("https://example.com")
                .slotCapacity(5)
                .depositAcorn(REWARD * 5)
                .rewardAcorn(REWARD)
                .startAt(now.minusDays(1))
                .endAt(now.plusDays(7))
                .project(project)
                .build());
    }

    private UUID saveFeedback(FeedbackPost post, String email, FeedbackStatus status) {
        Member tester = memberRepository.save(Member.createEmailMember(email, "test-password"));
        Feedback feedback = Feedback.create(post, tester);
        if (status != FeedbackStatus.WRITING) {
            feedback.submit(List.of());
        }
        if (status == FeedbackStatus.OBJECTED) {
            feedback.reject(new FeedbackRejectRequest(RejectType.LOW_EFFORT_ANSWER, "거절 사유".repeat(30)));
            feedback.object(tester.getId(), new FeedbackObjectRequest(ObjectType.FAITHFUL_ANSWER, "성실하게 작성했습니다."));
        }
        return feedbackRepository.save(feedback).getId();
    }
}
