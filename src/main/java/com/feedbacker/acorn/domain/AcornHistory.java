package com.feedbacker.acorn.domain;

import com.feedbacker.acorn.domain.type.AcornHistoryType;
import com.feedbacker.feedback.domain.Feedback;
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

    /** 도토리 증가 내역 */
    private static AcornHistory increase(
            Member member,
            AcornHistoryType type,
            UUID feedbackPostId,
            UUID feedbackId,
            int amount
    ) {
        return AcornHistory.builder()
                .member(member)
                .feedbackPostId(feedbackPostId)
                .feedbackId(feedbackId)
                .type(type)
                .changeAcorn(Math.abs(amount))
                .build();
    }

    /** 도토리 감소 내역 */
    private static AcornHistory decrease(
            Member member,
            AcornHistoryType type,
            UUID feedbackPostId,
            UUID feedbackId,
            int amount
    ) {
        return AcornHistory.builder()
                .member(member)
                .feedbackPostId(feedbackPostId)
                .feedbackId(feedbackId)
                .type(type)
                .changeAcorn(-Math.abs(amount))
                .build();
    }

    /** 기본 도토리 지급 메서드 */
    public static AcornHistory earn(Member member, AcornHistoryType type, Integer changeAcorn) {
        return increase(member, type, null, null, changeAcorn);
    }

    /** 기본 도토리 차감 메서드 */
    public static AcornHistory withdraw(Member member, AcornHistoryType type, Integer changeAcorn) {
        return decrease(member, type, null, null, changeAcorn);
    }

    /** 피드백 승인 시 테스터 보상 내역 */
    public static AcornHistory reward(Member tester, Feedback feedback, FeedbackPost feedbackPost) {
        return increase(tester, AcornHistoryType.FEEDBACK_ACCEPT, feedbackPost.getId(), feedback.getId(), feedbackPost.getRewardAcorn());
    }

    /** 모집글 등록 시 작성자 예치(차감) 내역 */
    public static AcornHistory deposit(Member writer, UUID feedbackPostId, Integer depositAcorn) {
        return decrease(writer, AcornHistoryType.FEEDBACK_DEPOSIT, feedbackPostId, null, depositAcorn);
    }

    /** 모집글 완료 시 작성자 환급 내역 */
    public static AcornHistory refund(Member writer, UUID feedbackPostId, Integer refundAcorn) {
        return increase(writer, AcornHistoryType.FEEDBACK_REFUND, feedbackPostId, null, refundAcorn);
    }

}
