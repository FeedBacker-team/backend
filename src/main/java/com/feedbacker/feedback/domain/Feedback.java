package com.feedbacker.feedback.domain;

import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectReasonType;
import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @Column(name = "feedback_post_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID feedbackPostId;

    @Column(name = "tester_id", nullable = false)
    private UUID testerId;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> answers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason_type")
    private RejectReasonType rejectReasonType;

    @Column(name = "reject_detail", columnDefinition = "TEXT")
    private String rejectDetail;

    @Column(nullable = false)
    private LocalDateTime submitAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Builder
    public Feedback(
            UUID feedbackPostId,
            FeedbackStatus status,
            LocalDateTime submitAt,
            LocalDateTime expireAt,
            List<QuestionAnswer> answers
    ) {
        this.feedbackPostId = feedbackPostId;
        this.status = status != null ? status : FeedbackStatus.SUBMITTED;
        this.submitAt = submitAt != null ? submitAt : LocalDateTime.now();
        this.expireAt = expireAt;

        if (answers != null) {
            answers.forEach(this::addAnswer);
        }
    }

    private void addAnswer(QuestionAnswer answer) {
        this.answers.add(answer);
        answer.setFeedback(this);
    }
}