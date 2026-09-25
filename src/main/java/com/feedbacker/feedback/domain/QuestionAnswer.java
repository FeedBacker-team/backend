package com.feedbacker.feedback.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.global.image.ImageInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", columnDefinition = "jsonb")
    private List<ImageInfo> images = new ArrayList<>();

    // 객관식 선택
    private List<Integer> selectedOption;

    // 주관식 답변
    @Column(columnDefinition = "TEXT")
    private String subjectiveAnswer;

    @Builder
    public QuestionAnswer(
            Feedback feedback,
            Long questionId,
            Integer questionOrder,
            List<ImageInfo> images,
            List<Integer> selectedOption,
            String subjectiveAnswer
    ) {
        this.feedback = feedback;
        this.questionId = questionId;
        this.questionOrder = questionOrder;
        this.images = images;
        this.selectedOption = selectedOption;
        this.subjectiveAnswer = subjectiveAnswer;
    }

}