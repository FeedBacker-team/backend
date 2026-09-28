package com.feedbacker.feedbackpost.domain.dto.response;

import java.util.List;

public record FeedbackFormResponse(
        List<ChoiceQuestionResponse> choiceQuestionResponses,
        List<SubjectiveQuestionResponse> subjectiveQuestionResponses
) {
}
