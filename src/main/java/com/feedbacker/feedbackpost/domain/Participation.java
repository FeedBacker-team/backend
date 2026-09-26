package com.feedbacker.feedbackpost.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.member.Member;
import com.feedbacker.feedbackpost.domain.type.ParticipationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "participations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_participation_post_tester",
                        columnNames = {
                                "feedback_post_id",
                                "tester_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_participation_tester",
                        columnList = "tester_id"
                ),
                @Index(
                        name = "idx_participation_post_status",
                        columnList = "feedback_post_id, status"
                ),
                @Index(
                        name = "idx_participation_deadline_status",
                        columnList = "submission_deadline_at, status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participation extends BaseTimeEntity {
    private static final long SUBMISSION_LIMIT_HOURS = 24L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_post_id", nullable = false)
    private FeedbackPost feedbackPost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tester_id", nullable = false)
    private Member tester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "submission_deadline_at", nullable = false)
    private LocalDateTime submissionDeadlineAt;

    @Column(name = "abandoned_at")
    private LocalDateTime abandonedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    private Participation(
            FeedbackPost feedbackPost,
            Member tester,
            LocalDateTime reservedAt
    ) {
        validateFeedbackPost(feedbackPost);
        validateTester(tester);
        validateReservedAt(reservedAt);

        this.feedbackPost = feedbackPost;
        this.tester = tester;
        this.status = ParticipationStatus.RESERVED;
        this.reservedAt = reservedAt;
        this.submissionDeadlineAt = reservedAt.plusHours(SUBMISSION_LIMIT_HOURS);
    }

    public static Participation reserve(
            FeedbackPost feedbackPost,
            Member tester
    ){
        return new Participation(
                feedbackPost,
                tester,
                LocalDateTime.now()
        );
    }

    public void submit(LocalDateTime submittedAt) {
        validateReservedStatus();

        if (submittedAt == null) {
            throw new IllegalArgumentException(
                    "제출 시각은 필수입니다."
            );
        }

        if (isDeadlineReached(submittedAt)) {
            throw new IllegalStateException(
                    "피드백 제출 기한이 지났습니다."
            );
        }

        this.status = ParticipationStatus.SUBMITTED;
    }

    public void abandon(LocalDateTime abandonedAt) {
        validateReservedStatus();

        if (abandonedAt == null) {
            throw new IllegalArgumentException(
                    "참여 포기 시각은 필수입니다."
            );
        }

        if (isDeadlineReached(abandonedAt)) {
            throw new IllegalStateException(
                    "이미 제출 기한이 지난 참여입니다."
            );
        }

        this.status = ParticipationStatus.ABANDONED;
        this.abandonedAt = abandonedAt;
    }

    public void expire(LocalDateTime expiredAt) {
        validateReservedStatus();

        if (expiredAt == null) {
            throw new IllegalArgumentException(
                    "만료 처리 시각은 필수입니다."
            );
        }

        if (!isDeadlineReached(expiredAt)) {
            throw new IllegalStateException(
                    "아직 제출 기한이 지나지 않았습니다."
            );
        }

        this.status = ParticipationStatus.EXPIRED;
        this.expiredAt = expiredAt;
    }

    public boolean isReserved() {
        return status == ParticipationStatus.RESERVED;
    }

    public boolean isSubmitted() {
        return status == ParticipationStatus.SUBMITTED;
    }

    public boolean isAbandoned() {
        return status == ParticipationStatus.ABANDONED;
    }

    public boolean isExpired() {
        return status == ParticipationStatus.EXPIRED;
    }

    public boolean isDeadlineReached(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "기준 시각은 필수입니다."
            );
        }

        return !now.isBefore(submissionDeadlineAt);
    }

    public boolean occupiesSlot() {
        return status == ParticipationStatus.RESERVED
                || status == ParticipationStatus.SUBMITTED;
    }

    private void validateReservedStatus() {
        if (status != ParticipationStatus.RESERVED) {
            throw new IllegalStateException(
                    "슬롯 예약 상태에서만 처리할 수 있습니다."
            );
        }
    }

    private void validateFeedbackPost(
            FeedbackPost feedbackPost
    ) {
        if (feedbackPost == null) {
            throw new IllegalArgumentException(
                    "QA 모집글은 필수입니다."
            );
        }
    }

    private void validateTester(Member tester) {
        if (tester == null) {
            throw new IllegalArgumentException(
                    "참여 회원은 필수입니다."
            );
        }
    }

    private void validateReservedAt(
            LocalDateTime reservedAt
    ) {
        if (reservedAt == null) {
            throw new IllegalArgumentException(
                    "슬롯 예약 시각은 필수입니다."
            );
        }
    }
}
