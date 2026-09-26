package com.feedbacker.feedbackpost.service.mapper;

import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.dto.response.ChoiceQuestionResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackFormResponse;
import com.feedbacker.feedbackpost.domain.dto.response.SubjectiveQuestionResponse;
import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.feedbackpost.service.ImageService;
import com.feedbacker.global.image.ImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedbackFormMapper {

    private final ImageService imageService;

    public FeedbackFormResponse toResponse(List<Question> questions) {
        List<ChoiceQuestionResponse> choices = new ArrayList<>();
        List<SubjectiveQuestionResponse> subjectives = new ArrayList<>();

        for (Question question : questions) {
            List<ImageResponse> images = imageService.toResponses(question.getImages());

            if (question.getQuestionType() == QuestionType.CHOICE) {
                choices.add(new ChoiceQuestionResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        question.getOptionTexts(),
                        question.getMaxSelectionCount(),
                        images,
                        question.isRequired()
                ));
            } else {
                subjectives.add(new SubjectiveQuestionResponse(
                        question.getOrder(),
                        question.getQuestionText(),
                        images,
                        question.isRequired(),
                        question.getMinimumLength()
                ));
            }
        }

        return new FeedbackFormResponse(choices, subjectives);
    }
}
