package com.feedbacker.project.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ProjectViewRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int insertIfAbsent(UUID projectId, UUID memberId) {
        return entityManager.createNativeQuery("""
                        INSERT INTO project_views(
                                                  project_id,
                                                  member_id,
                                                  viewed_at
                        )
                        VALUES (
                                :projectId,
                                :memberId,
                                CURRENT_TIMESTAMP
                        )
                        ON CONFLICT (project_id, member_id) DO NOTHING""")
                .setParameter("projectId", projectId)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }
}
