package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.domain.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findAllByFeedbackIdOrderByQuestionOrderAsc(UUID feedbackId);
}
