package com.feedbacker.feedback.service.mapper;

import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.ChoiceQuestionAnswerResponse;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
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
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class QuestionAnswerResponseMapper {

    private final ImageService imageService;

    public QuestionAnswerResponse toResponse(
            List<Question> questions,
            List<QuestionAnswer> questionAnswers
    ) {
        Map<Long, QuestionAnswer> answerMap = questionAnswers.stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, answer -> answer));

        List<ChoiceQuestionAnswerResponse> choices = new ArrayList<>();
        List<SubjectiveQuestionAnswerResponse> subjectives = new ArrayList<>();

        for (Question question : questions) {
            QuestionAnswer answer = answerMap.get(question.getId());
            List<ImageResponse> answerImages = answer == null
                    ? List.of()
                    : imageService.toResponses(answer.getImages());
            List<ImageResponse> images = Stream.concat(
                    imageService.toResponses(question.getImages()).stream(),
                    answerImages.stream()
            ).toList();

            if (question.getQuestionType() == QuestionType.CHOICE) {
                choices.add(new ChoiceQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        List.copyOf(question.getOptionTexts()),
                        List.copyOf(question.getOptionTexts()).size(),
                        answer == null ? null : normalizeSelectedOption(answer.getSelectedOption()),
                        images
                ));
            } else {
                subjectives.add(new SubjectiveQuestionAnswerResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        answer == null ? null : normalizeText(answer.getSubjectiveAnswer()),
                        images
                ));
            }
        }

        return new QuestionAnswerResponse(choices, subjectives);
    }

    private List<Integer> normalizeSelectedOption(List<Integer> selectedOption) {
        return selectedOption == null || selectedOption.isEmpty() ? null : selectedOption;
    }

    private String normalizeText(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
