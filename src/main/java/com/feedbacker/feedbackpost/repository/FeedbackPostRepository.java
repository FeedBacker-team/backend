package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeedbackPostRepository extends JpaRepository<FeedbackPost, UUID>, JpaSpecificationExecutor<FeedbackPost> {
    List<FeedbackPost> findAllByWriterId(UUID writerId);
}
