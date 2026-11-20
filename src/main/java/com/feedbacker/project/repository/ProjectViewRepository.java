package com.feedbacker.project.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ProjectViewRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int recordMemberView(UUID projectId, UUID memberId) {
        return entityManager.createNativeQuery("""
                        INSERT INTO project_views (project_id, member_id, viewed_at)
                        VALUES (:projectId, :memberId, CURRENT_TIMESTAMP)
                        ON CONFLICT (project_id, member_id)
                        DO UPDATE SET viewed_at = EXCLUDED.viewed_at
                        WHERE (project_views.viewed_at AT TIME ZONE 'Asia/Seoul')::date
                        < (EXCLUDED.viewed_at AT TIME ZONE 'Asia/Seoul')::date""")
                .setParameter("projectId", projectId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    public int recordGuestView(UUID projectId, UUID guestId) {
        return entityManager.createNativeQuery("""
            INSERT INTO project_guest_views (project_id, guest_id, viewed_at)
            VALUES (:projectId, :guestId, CURRENT_TIMESTAMP)
            ON CONFLICT (project_id, guest_id)
            DO UPDATE SET viewed_at = EXCLUDED.viewed_at
            WHERE (project_guest_views.viewed_at AT TIME ZONE 'Asia/Seoul')::date
                < (EXCLUDED.viewed_at AT TIME ZONE 'Asia/Seoul')::date
            """)
                .setParameter("projectId", projectId)
                .setParameter("guestId", guestId)
                .executeUpdate();
    }
}

