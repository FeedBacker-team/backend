package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackPostRepository extends JpaRepository<FeedbackPost, UUID>, JpaSpecificationExecutor<FeedbackPost> {
    List<FeedbackPost> findAllByWriterId(UUID writerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeedbackPost f where f.id = :id")
    Optional<FeedbackPost> findByIdForUpdate(@Param("id") UUID id);
}
