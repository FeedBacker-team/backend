package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<Question> getAllQuestion(UUID feedbackPostId) {
        return questionRepository.findAllByFeedbackPostIdOrderByOrderAsc(feedbackPostId);
    }
}
