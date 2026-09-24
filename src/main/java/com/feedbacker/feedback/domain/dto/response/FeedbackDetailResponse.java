package com.feedbacker.feedback.domain.dto.response;

import java.util.List;

public record FeedbackDetailResponse(
        List<ChoiceQuestionAnswerResponse> choiceQuestionAnswers,
        List<SubjectiveQuestionAnswerResponse> subjectiveQuestionAnswers
) {
}
