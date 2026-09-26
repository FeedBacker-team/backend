package com.feedbacker.project.repository;


import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @EntityGraph(attributePaths = {"owner", "tags"})
    Optional<Project> findByIdAndStatus(
            UUID projectId,
            ProjectStatus status
    );

    @EntityGraph(attributePaths = {"owner", "tags"})
    List<Project> findByOwner_IdAndStatusOrderByCreatedAtDesc(UUID ownerId, ProjectStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Project p
            where p.id = :projectId
              and p.status = :status
            """)
    Optional<Project> findForUpdate(@Param("projectId") UUID projectId, @Param("status") ProjectStatus status);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Project p
            set p.viewCount = p.viewCount + 1
            where p.id = :projectId
              and p.status = :status
            """)
    int increaseViewCount(
            @Param("projectId") UUID projectId,
            @Param("status") ProjectStatus status
    );

    @Query("""
        select p
        from Project p
        where p.owner.id = :memberId
          and p.status = :projectStatus
          and not exists (
              select fp.id
              from FeedbackPost fp
              where fp.project = p
                and fp.status in :activeStatuses
          )
        order by p.createdAt desc
        """)
    List<Project> findAvailableProjects(
            @Param("memberId") UUID memberId,
            @Param("projectStatus") ProjectStatus projectStatus,
            @Param("activeStatuses")
            Collection<FeedbackPostStatus> activeStatuses
    );

    @Query("""
        select fp
        from FeedbackPost fp
        where fp.project.id = :projectId
          and fp.status in :statuses
        order by fp.createdAt desc, fp.id desc
        """)
    List<FeedbackPost> findActiveQa(
            @Param("projectId") UUID projectId,
            @Param("statuses")
            Collection<FeedbackPostStatus> statuses,
            Pageable pageable
    );

    @Query("""
        select case when count(fp) > 0
                    then true
                    else false
               end
        from FeedbackPost fp
        where fp.project.id = :projectId
          and fp.status in :statuses
        """)
    boolean existsActiveQa(
            @Param("projectId") UUID projectId,
            @Param("statuses")
            Collection<FeedbackPostStatus> statuses
    );

    @Query("""
        select fp
        from FeedbackPost fp
        join fetch fp.project p
        where p.owner.id = :memberId
          and p.status = :projectStatus
          and fp.status in :activeStatuses
        order by fp.createdAt desc, fp.id desc
        """)
    List<FeedbackPost> findActiveQaByOwner(
            @Param("memberId")
            UUID memberId,

            @Param("projectStatus")
            ProjectStatus projectStatus,

            @Param("activeStatuses")
            Collection<FeedbackPostStatus> activeStatuses
    );
}
