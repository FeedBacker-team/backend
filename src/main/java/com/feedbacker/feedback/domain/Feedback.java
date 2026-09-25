package com.feedbacker.feedback.domain;

import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.member.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_id")
    private UUID id;

    @Column(name = "feedback_post_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID feedbackPostId;

    @Column(name = "tester_id", nullable = false)
    private UUID testerId;

    @Column
    private String testerName;

    @Column(nullable = false)
    private String postTitle;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> answers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status;

    @Setter
    @Column(nullable = false)
    private Integer rewardAcorn;

    @Enumerated(EnumType.STRING)
    @Column
    private RejectType rejectType;

    @Column(columnDefinition = "TEXT")
    private String rejectDetail;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String objectReason;

    @Column(nullable = false)
    private LocalDateTime submitAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime processedAt;

    @Builder
    public Feedback(
            UUID feedbackPostId,
            UUID testerId,
            String testerName,
            String postTitle,
            FeedbackStatus status,
            LocalDateTime submitAt,
            LocalDateTime expireAt,
            List<QuestionAnswer> answers
    ) {
        this.feedbackPostId = feedbackPostId;
        this.testerId = testerId;
        this.testerName = testerName;
        this.postTitle = postTitle;
        this.status = status != null ? status : FeedbackStatus.SUBMITTED;
        this.submitAt = submitAt != null ? submitAt : LocalDateTime.now();
        this.expireAt = expireAt != null ? expireAt : this.submitAt.plusDays(100);

        if (answers != null) {
            answers.forEach(this::addAnswer);
        }
    }

    public static Feedback create(
            FeedbackPost feedbackPost,
            Member tester,
            List<QuestionAnswer> answers
    ) {
        return Feedback.builder()
                .feedbackPostId(feedbackPost.getId())
                .testerId(tester.getId())
                .testerName(tester.getNickname())
                .postTitle(feedbackPost.getTitle())
                .status(FeedbackStatus.SUBMITTED)
                .answers(answers)
                .submitAt(LocalDateTime.now())
                .build();
    }

    private void addAnswer(QuestionAnswer answer) {
        this.answers.add(answer);
        answer.setFeedback(this);
    }

    public void accept(Integer rewardAcorn) {
        this.rewardAcorn = rewardAcorn;
        this.processedAt = LocalDateTime.now();
        this.status = FeedbackStatus.ACCEPTED;
    }

    public void reject(FeedbackRejectRequest request) {
        this.rejectType = request.rejectType();
        this.rejectDetail = request.rejectDetail();
        this.processedAt = LocalDateTime.now();
        this.status = FeedbackStatus.REJECTED;
    }
}