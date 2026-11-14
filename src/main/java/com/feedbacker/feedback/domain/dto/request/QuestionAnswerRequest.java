package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuestionAnswerRequest(
        @Valid
        List<@NotNull ChoiceQuestionAnswerRequest> choiceAnswers,

        @Valid
        List<@NotNull SubjectiveQuestionAnswerRequest> subjectiveAnswers
) {
    public QuestionAnswerRequest {
        choiceAnswers = choiceAnswers == null ? List.of() : choiceAnswers;
        subjectiveAnswers = subjectiveAnswers == null ? List.of() : subjectiveAnswers;
    }
}
