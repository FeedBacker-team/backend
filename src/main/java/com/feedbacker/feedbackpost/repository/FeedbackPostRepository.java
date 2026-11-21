package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackPostRepository extends JpaRepository<FeedbackPost, UUID>, JpaSpecificationExecutor<FeedbackPost> {
    List<FeedbackPost> findAllByWriterIdAndDeletedFalse(UUID writerId);

    Optional<FeedbackPost> findByIdAndDeletedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeedbackPost f where f.id = :id and f.deleted = false")
    Optional<FeedbackPost> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeedbackPost f where f.id = :id")
    Optional<FeedbackPost> findByIdIncludingDeletedForUpdate(@Param("id") UUID id);

    /** 작성자의 모든 모집글 id (소프트 삭제 포함). 락 순서를 고정하기 위해 id 순으로 정렬한다 */
    @Query("select f.id from FeedbackPost f where f.writerId = :writerId order by f.id")
    List<UUID> findIdsByWriterIdIncludingDeleted(@Param("writerId") UUID writerId);

    /**
     * 물리 삭제 대상 모집글 id.
     * 소프트 삭제된 지 threshold 이전 + 정산(완료) 상태 + 미해결(작성중/검토 대기/이의제기) 피드백 없음
     */
    @Query("select p.id from FeedbackPost p "
            + "where p.deleted = true "
            + "and p.deletedAt <= :threshold "
            + "and p.status = :status "
            + "and not exists (select 1 from Feedback f "
            + "where f.feedbackPost = p and f.status in :unresolvedStatuses)")
    List<UUID> findPurgeableIds(
            @Param("threshold") LocalDateTime threshold,
            @Param("status") FeedbackPostStatus status,
            @Param("unresolvedStatuses") Collection<FeedbackStatus> unresolvedStatuses
    );

    @Query("select f.id from FeedbackPost f where f.status in :statuses and f.endAt <= :now")
    List<UUID> findEndedIds(
            @Param("statuses") Collection<FeedbackPostStatus> statuses,
            @Param("now") LocalDateTime now
    );
}
