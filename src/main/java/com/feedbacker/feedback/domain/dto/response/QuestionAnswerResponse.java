package com.feedbacker.feedback.domain.dto.response;

import java.util.List;

public record QuestionAnswerResponse(
        List<ChoiceQuestionAnswerResponse> choiceQuestionAnswerResponses,
        List<SubjectiveQuestionAnswerResponse> subjectiveQuestionAnswerResponses
) {
}
