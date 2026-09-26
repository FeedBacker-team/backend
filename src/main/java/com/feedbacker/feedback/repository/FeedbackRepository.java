package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> getAllByTesterId(UUID testerId);

    List<Feedback> getAllByFeedbackPost(FeedbackPost feedbackPost);
}
