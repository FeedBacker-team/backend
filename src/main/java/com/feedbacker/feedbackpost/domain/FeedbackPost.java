package com.feedbacker.feedbackpost.domain;

import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.exception.ParticipationErrorCode;
import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.project.domain.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "feedback_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_post_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private UUID writerId;

    @Column(nullable = false)
    private String writerName;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 1000)
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackPostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(length = 2048)
    private String serviceUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<ImageInfo> images = new ArrayList<>();

    @OneToMany(mappedBy = "feedbackPost", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Column(nullable = false)
    private Integer slotCapacity;

    @Setter
    @Column(nullable = false)
    private Integer remainSlotCount;

    @Column(nullable = false)
    private Integer depositAcorn;

    @Column(nullable = false)
    private Integer rewardAcorn;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Builder
    public FeedbackPost(
            UUID writerId,
            String writerName,
            String title,
            String description,
            FeedbackPostStatus status,
            TargetType targetType,
            String serviceUrl,
            List<ImageInfo> images,
            List<Question> questions,
            Integer slotCapacity,
            Integer remainSlotCount,
            Integer depositAcorn,
            Integer rewardAcorn,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Project project
    ) {
        this.writerId = writerId;
        this.writerName = writerName;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : FeedbackPostStatus.RECRUITING;
        this.targetType = targetType;
        this.serviceUrl = serviceUrl;
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
        this.slotCapacity = slotCapacity;
        this.remainSlotCount = remainSlotCount;
        this.depositAcorn = depositAcorn;
        this.rewardAcorn = rewardAcorn;
        this.startAt = startAt;
        this.endAt = endAt;
        this.project = project;

        if (questions != null) {
            questions.forEach(this::addQuestion);
        }
    }

    private void addQuestion(Question question) {
        this.questions.add(question);
        question.setFeedbackPost(this);
    }

    public void minusRemainSlotCount() {
        if (this.remainSlotCount <= 0) {
            return;
        }
        this.remainSlotCount--;
        // 마지막 슬롯이 채워지면 모집마감으로
        if (this.remainSlotCount == 0 && this.status == FeedbackPostStatus.RECRUITING) {
            this.status = FeedbackPostStatus.CLOSED;
        }
    }

    public void plusRemainSlotCount() {
        if (this.remainSlotCount >= this.slotCapacity) {
            return;
        }
        this.remainSlotCount++;
        // 슬롯이 꽉 차서 마감됐던 글이면 다시 모집중으로
        if (this.status == FeedbackPostStatus.CLOSED && LocalDateTime.now().isBefore(this.endAt)) {
            this.status = FeedbackPostStatus.RECRUITING;
        }
    }

    public void complete(UUID memberId) {
        validateNotCompleted();
        validateAccessAuth(memberId);
        status = FeedbackPostStatus.COMPLETED;
    }

    public void validateIsWriter(UUID memberId) {
        if (this.writerId.equals(memberId)) {
            throw new BusinessException(ParticipationErrorCode.SELF_PARTICIPATION_NOT_ALLOWED);
        }
    }

    public void validateAccessAuth(UUID memberId) {
        if (!this.writerId.equals(memberId)) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_ACCESS_DENIED);
        }
    }

    public void validateRecruiting() {
        if (this.status != FeedbackPostStatus.RECRUITING) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_RECRUITING);
        }
    }

    /** 모집 기간(startAt <= now < endAt) 안인지 검증 (참여 시 사용) */
    public void validateRecruitingPeriod(LocalDateTime now) {
        if (now.isBefore(this.startAt)) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_STARTED);
        }
        if (!now.isBefore(this.endAt)) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_RECRUITMENT_ENDED);
        }
    }

    /** 이미 '완료'된 모집글이면 예외 (완료 처리·피드백 제출 시 사용)
     * 통과 : '모집중', '모집마감' 상태
     * 에러 : '완료' 상태
     */
    public void validateNotCompleted() {
        if (this.status == FeedbackPostStatus.COMPLETED) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_ALREADY_COMPLETED);
        }
    }

    /** '완료'되지 않은 모집글이면 예외 (결과 조회 시 사용)
     * 통과 : '완료' 상태
     * 에러 : '모집중', '모집마감' 상태
     */
    public void validateCompleted() {
        if (this.status != FeedbackPostStatus.COMPLETED) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_COMPLETED);
        }
    }

}
