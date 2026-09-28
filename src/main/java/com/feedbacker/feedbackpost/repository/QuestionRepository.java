package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedbackpost.domain.Question;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>{

    List<Question> findAllByFeedbackPostIdOrderByOrderAsc(UUID feedbackPostId);
}
