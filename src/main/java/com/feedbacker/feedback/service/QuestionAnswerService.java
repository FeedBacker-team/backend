package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.ChoiceQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.QuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.SubjectiveQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.feedback.service.mapper.QuestionAnswerResponseMapper;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.repository.QuestionRepository;
import com.feedbacker.global.image.ImageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class QuestionAnswerService {

    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuestionAnswerResponseMapper questionAnswerResponseMapper;

    @Transactional
    public List<QuestionAnswer> createAnswers(
            QuestionAnswerRequest questionAnswerRequest
    ) {

        List<ChoiceQuestionAnswerRequest> choiceAnswers = questionAnswerRequest.choiceAnswers();
        List<SubjectiveQuestionAnswerRequest> subjectiveAnswers = questionAnswerRequest.subjectiveAnswers();

        List<Long> questionIds = Stream.concat(
                choiceAnswers.stream().map(ChoiceQuestionAnswerRequest::order),
                subjectiveAnswers.stream().map(SubjectiveQuestionAnswerRequest::order)
        ).toList();

        Map<Long, Question> questionMap = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        List<QuestionAnswer> answers = new ArrayList<>();

        for (ChoiceQuestionAnswerRequest answer : choiceAnswers) {
            Question question = questionMap.get(answer.order());
            if (question == null) {
                throw new RuntimeException();
            }

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .images(answer.images().stream()
                            .map(ImageRequest::toImageInfo)
                            .toList())
                    .selectedOption(answer.selectedOption())
                    .build();

            answers.add(questionAnswer);
        }

        for (SubjectiveQuestionAnswerRequest answer : subjectiveAnswers) {
            Question question = questionMap.get(answer.order());
            if (question == null) {
                throw new RuntimeException();
            }

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .subjectiveAnswer(answer.text())
                    .build();

            answers.add(questionAnswer);
        }

        return answers;
    }

    public List<QuestionAnswer> getAllQuestionAnswer(UUID feedbackId) {
        return questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(feedbackId);
    }

    public QuestionAnswerResponse toResponse(List<Question> questions, List<QuestionAnswer> questionAnswers) {
        return questionAnswerResponseMapper.toResponse(questions, questionAnswers);
    }
}
