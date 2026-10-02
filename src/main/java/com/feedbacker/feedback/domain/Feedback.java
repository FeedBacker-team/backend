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
@Table(
        name = "feedbacks",
        // DB에서 feedback_post_id 와 tester_id의 조합은 유니크
        uniqueConstraints = @UniqueConstraint(name = "uk_feedback_post_tester", columnNames = {"feedback_post_id", "tester_id"}),
        // 인덱싱
        indexes = {
                @Index(name = "idx_feedback_tester", columnList = "tester_id"),
                @Index(name = "idx_feedback_status_expire", columnList = "status, expire_at")  // 만료 스케줄러용
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    private static final long SUBMISSION_LIMIT_HOURS = 24L;   // 참여 후 제출 기한
    private static final long RESPONSE_LIMIT_HOURS = 72L;     // 제출 후 작성자 응답 기한

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feedback_id")
    private UUID id;

    /** 피드백을 진행한 테스터 값 (외래키) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tester_id", nullable = false)
    private Member tester;

    /** 피드백을 진행한 피드백 모집글 값 (외래키) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_post_id", nullable = false)
    private FeedbackPost feedbackPost;

    /** 테스터가 작성한 질문 답변 리스트 */
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> answers = new ArrayList<>();

    /** 피드백의 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackStatus status;

    /** 보상으로 받은 도토리 수 */
    @Setter
    @Column
    private Integer rewardAcorn;

    /** 피드백 거부 받았을시 거부 유형 */
    @Enumerated(EnumType.STRING)
    @Column
    private RejectType rejectType;

    /** 거부 상세 사유 */
    @Column(columnDefinition = "TEXT")
    private String rejectDetail;

    /** 거부를 이의제기 할 시 상세 사유 */
    @Column(columnDefinition = "TEXT")
    private String objectReason;

    /** 피드백 모집글에 참여한 시각 */
    @Column(nullable = false)
    private LocalDateTime participateAt;

    /** 피드백 제출 시각 */
    @Column
    private LocalDateTime submitAt;

    /** 피드백 제출 마감 기한 */
    @Column(nullable = false)
    private LocalDateTime expireAt;

    /** 피드백 포기 시각 */
    @Column
    private LocalDateTime cancelAt;

    /** 피드백 제출 후 응답(수락/거절) 마감 기한*/
    @Column
    private LocalDateTime responseDeadLineAt;

    /** 피드백 제출 후 응답(수락/거절) 진행 시각*/
    @Column
    private LocalDateTime responseAt;

    @Builder
    public Feedback(
            Member tester,
            FeedbackPost feedbackPost,
            List<QuestionAnswer> answers
    ) {
        this.tester = tester;
        this.feedbackPost = feedbackPost;
        this.status = FeedbackStatus.SUBMITTED;
        this.participateAt = LocalDateTime.now();
        this.expireAt = this.participateAt.plusHours(SUBMISSION_LIMIT_HOURS);

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
                .tester(tester)
                .feedbackPost(feedbackPost)
                .answers(answers)
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
        markAccepted(rewardAcorn);
    }

    /** 응답 기한이 지난 '제출' 상태 피드백을 자동 승인 (기한 검증 없음) */
    public void autoAccept(Integer rewardAcorn) {
        if (this.status != FeedbackStatus.SUBMITTED) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ALREADY_PROCESSED);
        }
        markAccepted(rewardAcorn);
    }

    private void markAccepted(Integer rewardAcorn) {
        this.rewardAcorn = rewardAcorn;
        this.responseAt = LocalDateTime.now();
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
        this.responseAt = LocalDateTime.now();
        this.status = FeedbackStatus.REJECTED;
    }

    public void object(UUID memberId, String objectReason) {
        // 피드백 작성자인지 검증
        if (!this.tester.getId().equals(memberId)) {
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
