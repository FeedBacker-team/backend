package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.QuestionType;

public record QuestionConfigResponse(
        Integer totalCount,
        Integer choiceQuestionCount,
        Integer subjectiveCount,
        Integer estimatedMinutes
) {
    public static QuestionConfigResponse from(FeedbackPost feedbackPost) {
        Integer choiceCount = Math.toIntExact(
                feedbackPost.getQuestions().stream()
                        .filter(question -> question.getQuestionType() == QuestionType.CHOICE)
                        .count()
        );
        Integer subjectiveCount = Math.toIntExact(
                feedbackPost.getQuestions().stream()
                        .filter(question -> question.getQuestionType() == QuestionType.SUBJECTIVE)
                        .count()
        );
        Integer totalCount = choiceCount + subjectiveCount;
        Integer estimatedMinutes = choiceCount * 1 + subjectiveCount * 5;
        return new QuestionConfigResponse(
                totalCount,
                choiceCount,
                subjectiveCount,
                estimatedMinutes
        );
    }
}
