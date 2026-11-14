package com.feedbacker.project.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "PROJECT_VIEW_TEST_DB_URL", matches = "jdbc:postgresql:.*")
@DataJpaTest(properties = {
        "spring.datasource.url=${PROJECT_VIEW_TEST_DB_URL}",
        "spring.datasource.username=${PROJECT_VIEW_TEST_DB_USER}",
        "spring.datasource.password=${PROJECT_VIEW_TEST_DB_PASSWORD}",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ProjectViewRepository.class)
class ProjectViewRepositoryPostgresTest {
    @Autowired EntityManager em;
    @Autowired ProjectViewRepository views;

    @ParameterizedTest(name = "한국 날짜별 1회 집계: {0}")
    @ValueSource(strings = {"member", "guest"})
    void countsAgainAfterSeoulMidnightButNotTwiceOnTheSameDay(String viewerType) {
        boolean guest = viewerType.equals("guest");
        String table = guest ? "project_guest_views" : "project_views";
        String viewerColumn = guest ? "guest_id" : "member_id";
        java.util.function.ToIntBiFunction<UUID, UUID> record =
                guest ? views::recordGuestView : views::recordMemberView;
        // 임시 테이블은 테스트 트랜잭션 종료 시 제거되며 기존 데이터를 사용하지 않는다.
        em.createNativeQuery("""
                CREATE TEMP TABLE %1$s (
                    project_id uuid NOT NULL, %2$s uuid NOT NULL,
                    viewed_at timestamptz NOT NULL, PRIMARY KEY (project_id, %2$s)
                ) ON COMMIT DROP
                """.formatted(table, viewerColumn)).executeUpdate();
        UUID project = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        assertThat(record.applyAsInt(project, viewer)).isEqualTo(1);
        assertThat(record.applyAsInt(project, viewer)).isZero();

        em.createNativeQuery("""
                UPDATE pg_temp.%s
                SET viewed_at = (date_trunc('day', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')
                                 AT TIME ZONE 'Asia/Seoul') - interval '1 second'
                """.formatted(table)).executeUpdate();
        assertThat(record.applyAsInt(project, viewer)).isEqualTo(1);
        assertThat(record.applyAsInt(project, viewer)).isZero();
    }
}
