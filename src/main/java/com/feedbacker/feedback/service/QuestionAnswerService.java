package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.ChoiceQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.QuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.request.SubjectiveQuestionAnswerRequest;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.QuestionAnswerRepository;
import com.feedbacker.feedback.service.mapper.QuestionAnswerResponseMapper;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackResultResponse;
import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.feedbackpost.repository.QuestionRepository;
import com.feedbacker.feedbackpost.service.QuestionService;
import com.feedbacker.global.image.ImageRequest;
import com.feedbacker.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionAnswerService {

    private final QuestionService questionService;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuestionAnswerResponseMapper questionAnswerResponseMapper;

    @Transactional
    public List<QuestionAnswer> createAnswers(
            UUID feedbackPostId,
            QuestionAnswerRequest questionAnswerRequest
    ) {

        if (questionAnswerRequest == null) {
            throw new BusinessException(FeedbackErrorCode.INVALID_QUESTION_ANSWER);
        }

        List<Question> questions = questionRepository
                .findAllByFeedbackPostIdOrderByOrderAsc(feedbackPostId);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(question -> question.getOrder().longValue(), question -> question));

        Set<Long> seenQuestionIds = new HashSet<>();
        Set<Long> answeredQuestionIds = new HashSet<>();
        List<QuestionAnswer> answers = new ArrayList<>();

        for (ChoiceQuestionAnswerRequest answer : questionAnswerRequest.choiceAnswers()) {
            if (answer == null) {
                throw new BusinessException(FeedbackErrorCode.INVALID_QUESTION_ANSWER);
            }
            Question question = validateQuestion(questionMap, answer.order(), seenQuestionIds);
            if (question.getQuestionType() != QuestionType.CHOICE) {
                throw new BusinessException(FeedbackErrorCode.QUESTION_TYPE_MISMATCH);
            }

            List<Integer> selected = answer.selectedOption();
            int optionCount = question.getOptionTexts().size();
            if (selected.stream().anyMatch(option -> option == null || option < 1 || option > optionCount)) {
                throw new BusinessException(FeedbackErrorCode.INVALID_SELECTED_OPTION);
            }
            if (new HashSet<>(selected).size() != selected.size()) {
                throw new BusinessException(FeedbackErrorCode.DUPLICATE_SELECTED_OPTION);
            }
            if (selected.size() > question.getMaxSelectionCount()) {
                throw new BusinessException(FeedbackErrorCode.MAX_SELECTION_EXCEEDED);
            }
            if (!selected.isEmpty()) {
                answeredQuestionIds.add(question.getId());
            }

            answers.add(QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .images(answer.images().stream().map(ImageRequest::toImageInfo).toList())
                    .selectedOption(selected)
                    .build());
        }

        for (SubjectiveQuestionAnswerRequest answer : questionAnswerRequest.subjectiveAnswers()) {
            if (answer == null) {
                throw new BusinessException(FeedbackErrorCode.INVALID_QUESTION_ANSWER);
            }
            Question question = validateQuestion(questionMap, answer.order(), seenQuestionIds);
            if (question.getQuestionType() != QuestionType.SUBJECTIVE) {
                throw new BusinessException(FeedbackErrorCode.QUESTION_TYPE_MISMATCH);
            }

            String text = answer.text();
            boolean hasAnswer = text != null && !text.isBlank();
            if (hasAnswer) {
                Integer minimumLength = question.getMinimumLength();
                if (minimumLength != null && text.strip().length() < minimumLength) {
                    throw new BusinessException(FeedbackErrorCode.ANSWER_TOO_SHORT);
                }
                answeredQuestionIds.add(question.getId());
            }

            answers.add(QuestionAnswer.builder()
                    .questionId(question.getId())
                    .questionOrder(question.getOrder())
                    .subjectiveAnswer(text)
                    .images(List.of())
                    .build());
        }

        for (Question question : questions) {
            if (question.isRequired() && !answeredQuestionIds.contains(question.getId())) {
                throw new BusinessException(FeedbackErrorCode.REQUIRED_ANSWER_MISSING);
            }
        }
        return answers;
    }

    private Question validateQuestion(Map<Long, Question> questionMap, Long order, Set<Long> seenQuestionIds) {
        if (order == null || order < 1) {
            throw new BusinessException(FeedbackErrorCode.INVALID_QUESTION_ANSWER);
        }
        Question question = questionMap.get(order);
        if (question == null) {
            throw new BusinessException(FeedbackErrorCode.QUESTION_NOT_FOUND);
        }
        if (!seenQuestionIds.add(question.getId())) {
            throw new BusinessException(FeedbackErrorCode.DUPLICATE_QUESTION_ANSWER);
        }
        return question;
    }

    public List<FeedbackResultResponse> getFeedbackResults(List<Feedback> feedbacks, FeedbackPost feedbackPost) {
        return feedbacks.stream()
                .map(feedback -> {
                    QuestionAnswerResponse questionAnswer =
                            toResponse(
                                    questionService.getAllQuestion(feedbackPost.getId()),
                                    getAllQuestionAnswer(feedback.getId())
                            );

                    return FeedbackResultResponse.from(feedback, questionAnswer);
                })
                .toList();
    }

    public List<QuestionAnswer> getAllQuestionAnswer(UUID feedbackId) {
        return questionAnswerRepository.findAllByFeedbackIdOrderByQuestionOrderAsc(feedbackId);
    }

    public QuestionAnswerResponse toResponse(List<Question> questions, List<QuestionAnswer> questionAnswers) {
        return questionAnswerResponseMapper.toResponse(questions, questionAnswers);
    }
}
