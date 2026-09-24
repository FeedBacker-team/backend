package com.feedbacker.feedback.service.mapper;

import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.ChoiceQuestionAnswerResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.SubjectiveQuestionAnswerResponse;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.feedbackpost.service.ImageService;
import com.feedbacker.global.image.ImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FeedbackDetailMapper {

    private final ImageService imageService;

    public FeedbackDetailResponse toResponse(List<Question> questions, List<QuestionAnswer> questionAnswers) {
        Map<Long, QuestionAnswer> answerMap = questionAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, answer -> answer));

        List<ChoiceQuestionAnswerResponse> choices = new ArrayList<>();
        List<SubjectiveQuestionAnswerResponse> subjectives = new ArrayList<>();

        for (Question question : questions) {
            QuestionAnswer answer = answerMap.get(question.getId());
            List<ImageResponse> images = imageService.toResponses(question.getImages());

            if (question.getQuestionType() == QuestionType.CHOICE) {
                choices.add(new ChoiceQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        question.getOptionTexts(),
                        question.getOptionTexts().size(),
                        answer == null ? null : answer.getChoiceOption(),
                        images
                ));
            } else {
                subjectives.add(new SubjectiveQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        answer == null ? null : answer.getSubjectiveAnswer(),
                        images
                ));
            }
        }

        return new FeedbackDetailResponse(choices, subjectives);
    }
}
