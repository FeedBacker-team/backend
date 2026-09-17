package com.feedbacker.feedback.domain.dto.response;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FeedbackDetailResponse(
        List<ChoiceQuestionAnswerResponse> choiceQuestionAnswers,
        List<SubjectiveQuestionAnswerResponse> subjectiveQuestionAnswers
) {
    static public FeedbackDetailResponse from(List<Question> questions, List<QuestionAnswer> questionAnswers) {

        Map<Long, QuestionAnswer> answerMap = questionAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, qa -> qa));

        List<ChoiceQuestionAnswerResponse> choiceQuestionAnswers = new ArrayList<>();
        List<SubjectiveQuestionAnswerResponse> subjectiveQuestionAnswers = new ArrayList<>();

        for (Question question : questions) {
            QuestionAnswer answer = answerMap.get(question.getId());

            if (question.getQuestionType() == QuestionType.CHOICE) {
                choiceQuestionAnswers.add(new ChoiceQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        question.getOptionTexts(),
                        question.getOptionTexts().size(),
                        answer.getChoiceOption(),
                        null //imageUrl
                ));
            } else {
                subjectiveQuestionAnswers.add(new SubjectiveQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        answer.getSubjectiveAnswer(),
                        null //imageUrl
                ));
            }
        }

        return new FeedbackDetailResponse(choiceQuestionAnswers, subjectiveQuestionAnswers);
    }
}
