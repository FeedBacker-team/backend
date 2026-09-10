package com.feedbacker.feedbackpost.domain;

import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;import org.hibernate.annotations.JdbcTypeCode;import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;import java.util.List;import java.util.UUID;

@Entity
@Table(name = "feedback_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_post_id", updatable = false, nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackPostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    @Column(length = 3000)
    private String description;

    @Column(length = 2048)
    private String serviceUrl;

    @ElementCollection
    @CollectionTable(
            name = "feedback_post_images",
            joinColumns = @JoinColumn(name = "feedback_post_id")
    )
    @Column(name = "image_id")
    private List<Long> imageIds;

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

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "project_id", nullable = false)
//    private Project project;

    @Builder
    public FeedbackPost(
            String title,
            FeedbackPostStatus status,
            TargetType targetType,
            String description,
            String serviceUrl,
            List<Long> imageIds,
            List<Question> questions,
            Integer slotCapacity,
            Integer remainSlotCount,
            Integer depositAcorn,
            Integer rewardAcorn,
            LocalDateTime startAt,
            LocalDateTime endAt
//            Project project
    ) {
        this.title = title;
        this.status = status != null ? status : FeedbackPostStatus.RECRUITING;
        this.targetType = targetType;
        this.description = description;
        this.serviceUrl = serviceUrl;
        this.imageIds = imageIds;
        this.questions = questions;
        this.slotCapacity = slotCapacity;
        this.remainSlotCount = remainSlotCount;
        this.depositAcorn = depositAcorn;
        this.rewardAcorn = rewardAcorn;
        this.startAt = startAt;
        this.endAt = endAt;
//        this.project = project;
    }

}