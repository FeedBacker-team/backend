package com.feedbacker.feedbackpost.repository;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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
    List<FeedbackPost> findAllByWriterId(UUID writerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeedbackPost f where f.id = :id")
    Optional<FeedbackPost> findByIdForUpdate(@Param("id") UUID id);

    /** 참여 사전 검증용 조회. 락 없이 읽고, 읽기 전용이라 커밋 때 이 엔티티로 UPDATE가 나가지 않는다 */
    @QueryHints(@QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true"))
    @Query("select f from FeedbackPost f where f.id = :id")
    Optional<FeedbackPost> findByIdReadOnly(@Param("id") UUID id);

    /**
     * 슬롯 1개 차감 (조건부 UPDATE). 행 락은 이 UPDATE부터 커밋까지만 잡힌다.
     * 모집중이고 남은 슬롯이 있을 때만 차감하고, 마지막 슬롯이면 모집마감(CLOSED)으로 바꾼다.
     * SET 절의 f.remainSlotCount는 변경 전 값이다.
     * @return 차감된 행 수 (0이면 슬롯이 없거나 모집중이 아님)
     */
    @Modifying(flushAutomatically = true)
    @Query("update FeedbackPost f set "
            + "f.remainSlotCount = f.remainSlotCount - 1, "
            + "f.status = case when f.remainSlotCount = 1 then :closed else f.status end, "
            + "f.updatedAt = :now "
            + "where f.id = :id and f.status = :recruiting and f.remainSlotCount > 0")
    int decreaseRemainSlot(
            @Param("id") UUID id,
            @Param("recruiting") FeedbackPostStatus recruiting,
            @Param("closed") FeedbackPostStatus closed,
            @Param("now") LocalDateTime now
    );

    @Query("select f.id from FeedbackPost f where f.status in :statuses and f.endAt <= :now")
    List<UUID> findEndedIds(
            @Param("statuses") Collection<FeedbackPostStatus> statuses,
            @Param("now") LocalDateTime now
    );
}
