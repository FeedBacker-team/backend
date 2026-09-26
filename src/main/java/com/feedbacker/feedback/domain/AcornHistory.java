package com.feedbacker.feedback.domain;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
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

    @Column
    private Integer changeAcorn;

    @Column
    private Integer beforeAcorn;

    @Column
    private Integer afterAcron;

    @Builder
    public AcornHistory (
            Member member,
            UUID feedbackPostId,
            UUID feedbackId,
            Integer acronChange,
            Integer beforeAcorn,
            Integer afterAcron
    ) {
        this.member = member;
        this.feedbackPostId = feedbackPostId;
        this.feedbackId = feedbackId;
        this.changeAcorn = acronChange;
        this.beforeAcorn = beforeAcorn;
        this.afterAcron = afterAcron;
    }

    public static List<AcornHistory> create(
            Member tester,
            Member writer,
            Feedback feedback,
            FeedbackPost feedbackPost
    ) {
        List<AcornHistory> acornHistories = new ArrayList<>();
        Integer rewardAcorn = feedbackPost.getRewardAcorn();
        Integer testerBeforeAcorn = tester.getAcornWallet().getBalance();
        Integer writerBeforeAcorn = writer.getAcornWallet().getBalance();

        acornHistories.add(
                AcornHistory.builder()
                        .member(tester)
                        .feedbackPostId(feedbackPost.getId())
                        .feedbackId(feedback.getId())
                        .acronChange(rewardAcorn)
                        .beforeAcorn(testerBeforeAcorn)
                        .afterAcron(testerBeforeAcorn + rewardAcorn)
                        .build()
        );

        acornHistories.add(
                AcornHistory.builder()
                        .member(writer)
                        .feedbackPostId(feedbackPost.getId())
                        .feedbackId(feedback.getId())
                        .acronChange(rewardAcorn)
                        .beforeAcorn(testerBeforeAcorn)
                        .afterAcron(writerBeforeAcorn - rewardAcorn)
                        .build()
        );

        return acornHistories;
    }

}
