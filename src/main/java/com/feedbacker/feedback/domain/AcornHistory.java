package com.feedbacker.feedback.domain;

import com.feedbacker.feedback.domain.type.AcornHistoryType;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcornHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acorn_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column
    private UUID feedbackPostId;

    @Column
    private UUID feedbackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcornHistoryType type;

    @Column(nullable = false)
    private LocalDateTime translateAt;

    @Column(nullable = false)
    private Integer changeAcorn;

    @Builder
    public AcornHistory (
            Member member,
            UUID feedbackPostId,
            UUID feedbackId,
            AcornHistoryType type,
            Integer changeAcorn
    ) {
        this.member = member;
        this.feedbackPostId = feedbackPostId;
        this.feedbackId = feedbackId;
        this.type = type;
        this.translateAt = LocalDateTime.now();
        this.changeAcorn = changeAcorn;
    }

    /** 피드백 승인 시 테스터 보상 내역 */
    public static AcornHistory reward(Member tester, Feedback feedback, FeedbackPost feedbackPost) {
        return AcornHistory.builder()
                .member(tester)
                .feedbackPostId(feedbackPost.getId())
                .feedbackId(feedback.getId())
                .type(AcornHistoryType.FEEDBACK_ACCEPT)
                .changeAcorn(feedbackPost.getRewardAcorn())
                .build();
    }

    /** 모집글 등록 시 작성자 예치(차감) 내역 */
    public static AcornHistory deposit(Member writer, UUID feedbackPostId, Integer depositAcorn) {
        return AcornHistory.builder()
                .member(writer)
                .feedbackPostId(feedbackPostId)
                .type(AcornHistoryType.FEEDBACK_DEPOSIT)
                .changeAcorn(-depositAcorn)
                .build();
    }

    /** 모집글 완료 시 작성자 환급 내역 */
    public static AcornHistory refund(Member writer, UUID feedbackPostId, Integer refundAcorn) {
        return AcornHistory.builder()
                .member(writer)
                .feedbackPostId(feedbackPostId)
                .type(AcornHistoryType.FEEDBACK_REFUND)
                .changeAcorn(refundAcorn)
                .build();
    }

}
