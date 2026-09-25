package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.Valid;
import java.util.List;

public record QuestionAnswerRequest(
        @Valid
        List<ChoiceQuestionAnswerRequest> choiceAnswers,

        @Valid
        List<SubjectiveQuestionAnswerRequest> subjectiveAnswers
) {
}
