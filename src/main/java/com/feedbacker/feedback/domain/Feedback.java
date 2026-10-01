package com.feedbacker.feedback.domain;

import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.domain.type.RejectType;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.global.exception.BusinessException;
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

    @Column(name = "feedback_post_id", nullable = false, columnDefinition = "uuid")
    private UUID feedbackPostId;

    @Column(name = "tester_id", nullable = false)
    private UUID testerId;

    @Column
    private String testerName;

    @Column(nullable = false)
    private String postTitle;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> answers = new ArrayList<>();

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status;

    @Setter
    @Column
    private Integer rewardAcorn;

    @Enumerated(EnumType.STRING)
    @Column
    private RejectType rejectType;

    @Column(columnDefinition = "TEXT")
    private String rejectDetail;

    @Column(columnDefinition = "TEXT")
    private String objectReason;

    @Column(nullable = false)
    private LocalDateTime submitAt;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Column
    private LocalDateTime responseDeadLineAt;

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
            List<QuestionAnswer> answers
    ) {
        this.feedbackPostId = feedbackPostId;
        this.testerId = testerId;
        this.testerName = testerName;
        this.postTitle = postTitle;
        this.status = status != null ? status : FeedbackStatus.SUBMITTED;
        this.submitAt = submitAt != null ? submitAt : LocalDateTime.now();
        this.expireAt = this.submitAt.plusHours(24);
        this.responseDeadLineAt = this.submitAt.plusHours(72);

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
        // 피드백이 '제출'상태가 아닌지 검증
        if (this.status != FeedbackStatus.SUBMITTED) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ALREADY_PROCESSED);
        }
        // 응답 기한이 지났는지 검증
        if (this.responseDeadLineAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_PROCESS_EXPIRED);
        }
        this.rewardAcorn = rewardAcorn;
        this.processedAt = LocalDateTime.now();
        this.status = FeedbackStatus.ACCEPTED;
    }

    public void reject(FeedbackRejectRequest request) {
        // 피드백이 '제출'상태가 아닌지 검증
        if (this.status != FeedbackStatus.SUBMITTED) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ALREADY_PROCESSED);
        }
        // 응답 기한이 지났는지 검증
        if (this.responseDeadLineAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_PROCESS_EXPIRED);
        }
        // '거절 사유' 텍스트 앞뒤 공백 제거후 글자수 검증
        String cleaned = request.rejectDetail().strip();
        if (cleaned.length() < 100) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_REJECT_DETAIL_TOO_SHORT);
        }
        this.rejectType = request.rejectType();
        this.rejectDetail = cleaned;
        this.processedAt = LocalDateTime.now();
        this.status = FeedbackStatus.REJECTED;
    }

    public void object(UUID memberId, String objectReason) {
        // 피드백 작성자인지 검증
        if (!this.testerId.equals(memberId)) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
        // 피드백 상태가 '거절'상태가 아닌지 검증
        if (this.status != FeedbackStatus.REJECTED) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_NOT_REJECTED);
        }
        // 이전에 이의제기를 작성했는지 검증
        if (this.objectReason != null) {
            throw new BusinessException(FeedbackErrorCode.OBJECTION_ALREADY_SUBMITTED);
        }
        this.objectReason = objectReason;
    }
}
