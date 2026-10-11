package com.feedbacker.project.service;

import com.feedbacker.global.storage.SupabaseStorageService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectSort;
import com.feedbacker.project.domain.ProjectTag;
import com.feedbacker.project.domain.dto.response.ProjectCardResponse;
import com.feedbacker.project.domain.dto.response.ProjectListResponse;
import com.feedbacker.project.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import com.feedbacker.project.repository.ProjectViewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.stream.Stream;

import static com.feedbacker.project.domain.ProjectTag.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:project-tag-filter;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("프로젝트 태그 AND 필터 실제 조회")
class ProjectTagFilterTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager entityManager;

    private ValidatorFactory validatorFactory;
    private ProjectService projectService;
    private Member owner;


    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        projectService = new ProjectService(
                projectRepository, memberRepository, validatorFactory.getValidator(),
                mock(SupabaseStorageService.class), mock(ProjectViewRepository.class)
        );
        owner = memberRepository.save(Member.createEmailMember("tags@example.com", "test-password"));

        save("웹 전용", WEB);
        save("앱 전용", APP);
        save("웹 앱", WEB, APP);
        save("웹 앱 AI", WEB, APP, AI);
        save("AI 전용", AI);
        save("태그 없음");
        Project deleted = save("삭제된 웹 앱", WEB, APP);
        deleted.delete();
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("웹과 앱 선택 시 둘 다 포함한 프로젝트만 반환하고 추가 태그는 허용한다")
    void allSelectedTagsMustMatch() {
        ProjectListResponse result = search(List.of(WEB, APP));

        assertThat(result.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyInAnyOrder("웹 앱", "웹 앱 AI");
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("태그 하나를 선택하면 그 태그를 가진 모든 공개 프로젝트를 반환한다")
    void singleTag() {
        assertThat(search(List.of(WEB)).projects()).extracting(ProjectCardResponse::title)
                .containsExactlyInAnyOrder("웹 전용", "웹 앱", "웹 앱 AI");
    }

    @Test
    @DisplayName("태그 세 개를 선택하면 세 개 모두 포함해야 한다")
    void threeTags() {
        assertThat(search(List.of(WEB, APP, AI)).projects()).extracting(ProjectCardResponse::title)
                .containsExactly("웹 앱 AI");
    }

    @Test
    @DisplayName("선택 태그 순서는 조회 결과에 영향을 주지 않는다")
    void tagOrderDoesNotMatter() {
        assertThat(search(List.of(APP, WEB)).projects())
                .isEqualTo(search(List.of(WEB, APP)).projects());
    }

    @Test
    @DisplayName("모든 태그를 만족하는 프로젝트가 없으면 빈 목록을 반환한다")
    void noMatchingProject() {
        ProjectListResponse result = search(List.of(WEB, CLOUD));

        assertThat(result.projects()).isEmpty();
        assertThat(result.totalCount()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("태그 생략과 빈 목록은 태그 없는 프로젝트를 포함한 전체 공개 목록을 반환한다")
    void noTagFilter() {
        for (ProjectListResponse result : List.of(search(null), search(List.of()))) {
            assertThat(result.projects()).extracting(ProjectCardResponse::title)
                    .containsExactlyInAnyOrder(
                            "웹 전용", "앱 전용", "웹 앱", "웹 앱 AI", "AI 전용", "태그 없음"
                    );
            assertThat(result.totalCount()).isEqualTo(6);
        }
    }

    @Test
    @DisplayName("키워드 조건과 태그 AND 조건을 동시에 적용한다")
    void keywordAndTags() {
        ProjectListResponse result = projectService.getProjects(
                "AI", List.of(WEB, APP), ProjectSort.LATEST, 0, 12
        );

        assertThat(result.projects()).extracting(ProjectCardResponse::title)
                .containsExactly("웹 앱 AI");
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("페이지 전체 건수와 다음 페이지 여부도 모든 태그를 만족하는 프로젝트 기준이다")
    void paginationCountsOnlyMatchingProjects() {
        ProjectListResponse first = projectService.getProjects(
                null, List.of(WEB, APP), ProjectSort.LATEST, 0, 1);
        ProjectListResponse second = projectService.getProjects(
                null, List.of(WEB, APP), ProjectSort.LATEST, 1, 1);
        ProjectListResponse beyondLast = projectService.getProjects(
                null, List.of(WEB, APP), ProjectSort.LATEST, 2, 1);

        assertThat(first.totalCount()).isEqualTo(2);
        assertThat(second.totalCount()).isEqualTo(2);
        assertThat(first.page()).isZero();
        assertThat(second.page()).isEqualTo(1);
        assertThat(first.size()).isEqualTo(1);
        assertThat(first.projects()).hasSize(1);
        assertThat(second.projects()).hasSize(1);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isFalse();
        assertThat(Stream.concat(first.projects().stream(), second.projects().stream())
                .map(ProjectCardResponse::title).toList())
                .containsExactlyInAnyOrder("웹 앱", "웹 앱 AI");
        assertThat(beyondLast.projects()).isEmpty();
        assertThat(beyondLast.totalCount()).isEqualTo(2);
        assertThat(beyondLast.hasNext()).isFalse();
    }

    private ProjectListResponse search(List<ProjectTag> tags) {
        return projectService.getProjects(null, tags, ProjectSort.LATEST, 0, 12);
    }

    private Project save(String title, ProjectTag... tags) {
        return projectRepository.save(Project.create(
                owner, title, "태그 검색 테스트용 설명", List.of(tags),
                "https://example.com", "https://example.com/thumbnail.png"
        ));
    }
}
