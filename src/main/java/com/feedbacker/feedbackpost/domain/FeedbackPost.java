package com.feedbacker.feedbackpost.domain;

import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
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
    @Column(name = "feedback_post_id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column
    private UUID writerId;

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
        this.remainSlotCount--;
        if (this.remainSlotCount <= 0) {
            this.status = FeedbackPostStatus.CLOSED;
        }
    }

    public void complete() {
        if (status != FeedbackPostStatus.RECRUITING) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_RECRUITING);
        }

        status = FeedbackPostStatus.COMPLETED;
    }

    public void validateWriter(UUID id) {
        if (this.writerId == id) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_ACCESS_DENIED);
        }
    }

    public void validateRecruiting() {
        if (this.status != FeedbackPostStatus.RECRUITING) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_RECRUITING);
        }
    }

}
