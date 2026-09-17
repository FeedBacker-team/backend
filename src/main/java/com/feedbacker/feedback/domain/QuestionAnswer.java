package com.feedbacker.feedback.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_answer_id")
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    // 객관식 선택
    private Integer choiceOption;

    // 주관식 답변
    @Column(columnDefinition = "TEXT")
    private String subjectiveAnswer;

    @Builder
    public QuestionAnswer(
            Feedback feedback,
            Long questionId,
            Integer questionOrder,
            Integer choiceOption,
            String subjectiveAnswer
    ) {
        this.feedback = feedback;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.choiceOption = choiceOption;
        this.subjectiveAnswer = subjectiveAnswer;
    }

}