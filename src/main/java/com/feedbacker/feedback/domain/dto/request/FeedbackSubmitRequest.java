package com.feedbacker.feedback.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FeedbackSubmitRequest(

        @NotNull(message = "피드백 모집글 아이디는 필수입니다.")
        UUID feedbackPostId,

        @Valid
        List<ChoiceQuestionAnswerRequest> choiceAnswers,

        @Valid
        List<SubjectiveQuestionAnswerRequest> subjectiveAnswers
) {
}
