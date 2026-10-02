package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Feedback f where f.id = :id")
    Optional<Feedback> findByIdForUpdate(@Param("id") UUID id);

    List<Feedback> findAllByTesterId(UUID testerId);

    Optional<Feedback> findByFeedbackPostIdAndTesterId(UUID feedbackPostId, UUID memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Feedback f where f.feedbackPost.id = :feedbackPostId and f.tester.id = :testerId")
    Optional<Feedback> findByFeedbackPostIdAndTesterIdForUpdate(
            @Param("feedbackPostId") UUID feedbackPostId,
            @Param("testerId") UUID testerId
    );

    List<Feedback> findAllByFeedbackPostId(UUID feedbackPostId);

    boolean existsByFeedbackPostIdAndStatus(UUID feedbackPostId, FeedbackStatus status);

    @Query("select distinct f.feedbackPost.id from Feedback f "
            + "where f.status = :status and f.responseDeadLineAt <= :now")
    List<UUID> findFeedbackPostIdsWithOverdue(
            @Param("status") FeedbackStatus status,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Feedback f "
            + "where f.feedbackPost.id = :feedbackPostId "
            + "and f.status = :status and f.responseDeadLineAt <= :now")
    List<Feedback> findOverdueForUpdate(
            @Param("feedbackPostId") UUID feedbackPostId,
            @Param("status") FeedbackStatus status,
            @Param("now") LocalDateTime now
    );
}
