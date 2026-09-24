package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    List<Participation> findAllByFeedbackPost(FeedbackPost feedbackPost);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participation p where p.feedbackPost.id = :postId")
    List<Participation> findForUpdate(@Param("postId") UUID postId);

    Optional<Participation> findByFeedbackPostAndTester_Id(
            FeedbackPost feedbackPost,
            UUID testerId
    );
}
