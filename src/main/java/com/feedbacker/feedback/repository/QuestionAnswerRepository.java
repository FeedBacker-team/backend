package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.domain.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findAllByFeedbackIdOrderByQuestionOrderAsc(UUID feedbackId);

    List<QuestionAnswer> findAllByFeedbackIdIn(Collection<UUID> feedbackIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from QuestionAnswer qa where qa.feedback.id in :feedbackIds")
    int deleteAllByFeedbackIdIn(@Param("feedbackIds") Collection<UUID> feedbackIds);
}
