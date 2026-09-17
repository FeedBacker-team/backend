package com.feedbacker.feedbackpost.domain;

import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_post_id", nullable = false)
    private FeedbackPost feedbackPost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "question_order", nullable = false)
    private Integer order;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false, length = 500)
    private String questionText;

    @ElementCollection
    @CollectionTable(name = "question_images", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "image_id")
    private List<String> imageIds = new ArrayList<>();

    // 객관식 전용 보기 목록
    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> optionTexts = new ArrayList<>();

    // 주관식 전용 최소 글자 수
    private Integer minimumLength;

    @Builder
    public Question(
            FeedbackPost feedbackPost,
            QuestionType questionType,
            Integer order,
            boolean required,
            String questionText,
            List<String> imageIds,
            List<String> optionTexts,
            Integer minimumLength
    ) {
        this.feedbackPost = feedbackPost;
        this.questionType = questionType;
        this.order = order;
        this.required = required;
        this.questionText = questionText;
        this.imageIds = imageIds;
        this.optionTexts = optionTexts;
        this.minimumLength = minimumLength;
    }
}