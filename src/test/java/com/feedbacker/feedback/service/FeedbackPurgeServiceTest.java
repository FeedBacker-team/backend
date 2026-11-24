package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.image.ImageType;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.feedbacker.project.domain.ProjectTag.WEB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedback-purge;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("모집글·테스터가 모두 삭제된 피드백 물리 삭제")
class FeedbackPurgeServiceTest {

    private static final int REWARD = 10;
    private static final int BATCH_SIZE = 100;

    @Autowired FeedbackPostRepository feedbackPostRepository;
    @Autowired FeedbackRepository feedbackRepository;
    @Autowired QuestionAnswerRepository questionAnswerRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired EntityManager entityManager;

    private SupabaseStorageService storageService;
    private FeedbackPurgeService purgeService;
    private FeedbackPost post;

    @BeforeEach
    void setUp() {
        storageService = mock(SupabaseStorageService.class);
        purgeService = new FeedbackPurgeService(feedbackRepository, questionAnswerRepository, storageService);
        Member writer = memberRepository.save(Member.createEmailMember("writer@example.com", "test-password"));
        Project project = projectRepository.save(Project.create(
                writer, "프로젝트", "설명", List.of(WEB),
                "https://example.com", "https://example.com/thumbnail.png"));
        LocalDateTime now = LocalDateTime.now();
        post = feedbackPostRepository.save(FeedbackPost.builder()
                .writerId(writer.getId())
                .writerName("작성자")
                .title("모집글")
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

    @Test
    @DisplayName("모집글과 테스터가 모두 없는 피드백만 답변과 함께 지우고, 답변 이미지는 커밋 후 스토리지에서 지운다")
    void purge() {
        UUID orphanId = saveAcceptedFeedback("orphan@example.com", "answers/orphan.png");
        UUID postOnlyDeletedId = saveAcceptedFeedback("post-only@example.com", "answers/post-only.png");
        UUID testerOnlyDeletedId = saveAcceptedFeedback("tester-only@example.com", "answers/tester-only.png");
        UUID unresolvedOrphanId = saveSubmittedFeedback("unresolved@example.com");
        entityManager.flush();
        nullifyFks(orphanId, true, true);
        nullifyFks(postOnlyDeletedId, true, false);
        nullifyFks(testerOnlyDeletedId, false, true);
        nullifyFks(unresolvedOrphanId, true, true);
        entityManager.clear();

        List<UUID> ids = purgeService.getPurgeableIds(BATCH_SIZE);
        assertThat(ids).containsExactly(orphanId);
        assertThat(purgeService.purge(ids)).isEqualTo(1);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        entityManager.clear();

        assertThat(feedbackRepository.findById(orphanId)).isEmpty();
        assertThat(questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(orphanId)).isEmpty();
        assertThat(feedbackRepository.findAllById(List.of(postOnlyDeletedId, testerOnlyDeletedId, unresolvedOrphanId)))
                .hasSize(3);
        assertThat(questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(postOnlyDeletedId)).hasSize(1);
        verify(storageService).deleteImages(List.of("answers/orphan.png"));
    }

    @Test
    @DisplayName("조회 이후 삭제 조건을 만족하지 않게 된 피드백은 지우지 않는다")
    void purgeSkipsNonOrphan() {
        UUID feedbackId = saveAcceptedFeedback("tester@example.com", "answers/tester.png");
        entityManager.flush();
        entityManager.clear();

        assertThat(purgeService.purge(List.of(feedbackId))).isZero();
        assertThat(feedbackRepository.findById(feedbackId)).isPresent();
    }

    private UUID saveAcceptedFeedback(String email, String imagePath) {
        Member tester = memberRepository.save(Member.createEmailMember(email, "test-password"));
        Feedback feedback = Feedback.create(post, tester);
        feedback.submit(List.of(QuestionAnswer.builder()
                .questionId(1L)
                .questionOrder(1)
                .images(List.of(new ImageInfo(ImageType.ANSWER, 1, imagePath)))
                .subjectiveAnswer("답변")
                .build()));
        feedback.autoAccept(REWARD);
        return feedbackRepository.save(feedback).getId();
    }

    private UUID saveSubmittedFeedback(String email) {
        Member tester = memberRepository.save(Member.createEmailMember(email, "test-password"));
        Feedback feedback = Feedback.create(post, tester);
        feedback.submit(List.of());
        return feedbackRepository.save(feedback).getId();
    }

    /** 모집글/회원 삭제 시 DB의 ON DELETE SET NULL과 같은 상태를 만든다 */
    private void nullifyFks(UUID feedbackId, boolean post, boolean tester) {
        if (post) {
            entityManager.createNativeQuery("update feedbacks set feedback_post_id = null where feedback_id = :id")
                    .setParameter("id", feedbackId).executeUpdate();
        }
        if (tester) {
            entityManager.createNativeQuery("update feedbacks set tester_id = null where feedback_id = :id")
                    .setParameter("id", feedbackId).executeUpdate();
        }
    }
}
