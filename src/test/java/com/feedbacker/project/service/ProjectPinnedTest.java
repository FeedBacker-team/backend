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
import com.feedbacker.project.repository.ProjectViewRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.feedbacker.project.domain.ProjectTag.APP;
import static com.feedbacker.project.domain.ProjectTag.WEB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:project-pinned;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("프로젝트 고정 정렬 실제 조회")
class ProjectPinnedTest {
    @Autowired ProjectRepository projectRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager entityManager;

    private ProjectService projectService;
    private ValidatorFactory validatorFactory;
    private Member owner;
    private UUID olderPinnedId;
    private UUID newerPinnedId;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        projectService = new ProjectService(
                projectRepository, memberRepository, validatorFactory.getValidator(),
                mock(SupabaseStorageService.class), mock(ProjectViewRepository.class)
        );
        owner = memberRepository.save(Member.createEmailMember("pinned@example.com", "test-password"));
        olderPinnedId = saveFixture("고정 이전", true, 1, 10, WEB, APP);
        newerPinnedId = saveFixture("고정 최신", true, 2, 5, WEB, APP);
        saveFixture("일반 이전", false, 3, 200, WEB, APP);
        saveFixture("일반 최신", false, 4, 100, WEB);
        UUID deletedId = saveFixture("삭제 고정", true, 5, 300, WEB, APP);
        projectRepository.findById(deletedId).orElseThrow().delete();
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("고정 프로젝트가 먼저 나오고 각 그룹 안에서는 요청한 정렬을 유지한다")
    void pinnedFirstWithRequestedSort(ProjectSort sort) {
        ProjectListResponse result = search(null, null, sort, 0, 12);
        assertThat(result.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyElementsOf(expectedOrder(sort));
        assertThat(result.totalCount()).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("고정 프로젝트도 키워드 검색 조건에 맞아야 표시한다")
    void pinnedDoesNotBypassKeyword(ProjectSort sort) {
        ProjectListResponse result = search("일반", null, sort, 0, 12);
        assertThat(result.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyElementsOf(expectedOrder(sort).subList(2, 4));
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("태그 AND 필터와 고정 정렬을 함께 적용한다")
    void pinnedWithAllTagsFilter(ProjectSort sort) {
        // WEB만 가진 프로젝트를 고정해도 WEB+APP 검색에는 나타나지 않아야 한다.
        entityManager.createQuery("update Project p set p.pinned = true where p.title = :title")
                .setParameter("title", "일반 최신").executeUpdate();
        entityManager.clear();
        ProjectListResponse result = search(null, List.of(WEB, APP), sort, 0, 12);
        assertThat(result.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyElementsOf(expectedOrder(sort).stream()
                        .filter(title -> !title.equals("일반 최신")).toList());
        assertThat(result.totalCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("고정 순서를 반영한 뒤 페이징하므로 다음 페이지에 고정 항목을 반복하지 않는다")
    void paginationAfterPinnedSort(ProjectSort sort) {
        ProjectListResponse first = search(null, null, sort, 0, 2);
        ProjectListResponse second = search(null, null, sort, 1, 2);
        assertThat(first.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyElementsOf(expectedOrder(sort).subList(0, 2));
        assertThat(second.projects()).extracting(ProjectCardResponse::title)
                .containsExactlyElementsOf(expectedOrder(sort).subList(2, 4));
        assertThat(first.totalCount()).isEqualTo(4);
        assertThat(second.totalCount()).isEqualTo(4);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(ProjectSort.class)
    @DisplayName("고정을 해제하면 일반 정렬 순서로 돌아간다")
    void unpinRestoresRequestedSort(ProjectSort sort) {
        entityManager.createQuery("update Project p set p.pinned = false where p.id in :ids")
                .setParameter("ids", List.of(olderPinnedId, newerPinnedId)).executeUpdate();
        entityManager.clear();
        List<String> expected = sort == ProjectSort.LATEST
                ? List.of("일반 최신", "일반 이전", "고정 최신", "고정 이전")
                : List.of("일반 이전", "일반 최신", "고정 이전", "고정 최신");
        assertThat(search(null, null, sort, 0, 12).projects())
                .extracting(ProjectCardResponse::title).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("새 프로젝트의 고정 여부는 기본 false로 저장된다")
    void newProjectDefaultsToNotPinned() {
        Project project = projectRepository.save(Project.create(
                owner, "새 프로젝트", "설명", List.of(WEB),
                "https://example.com", "https://example.com/thumbnail.png"));
        entityManager.flush();
        entityManager.clear();
        assertThat(projectRepository.findById(project.getId()).orElseThrow().isPinned()).isFalse();
    }

    private List<String> expectedOrder(ProjectSort sort) {
        return sort == ProjectSort.LATEST
                ? List.of("고정 최신", "고정 이전", "일반 최신", "일반 이전")
                : List.of("고정 이전", "고정 최신", "일반 이전", "일반 최신");
    }

    private ProjectListResponse search(String keyword, List<ProjectTag> tags,
                                       ProjectSort sort, int page, int size) {
        return projectService.getProjects(keyword, tags, sort, page, size);
    }

    private UUID saveFixture(String title, boolean pinned, int day, long views, ProjectTag... tags) {
        Project project = projectRepository.save(Project.create(
                owner, title, "고정 정렬 테스트", List.of(tags),
                "https://example.com", "https://example.com/thumbnail.png"));
        entityManager.flush();
        // 운영과 동일하게 DB에서 고정 여부를 지정하고, 정렬 검증용 일시와 조회수를 설정한다.
        entityManager.createQuery("""
                update Project p
                set p.pinned = :pinned, p.createdAt = :createdAt, p.viewCount = :views
                where p.id = :id
                """)
                .setParameter("pinned", pinned)
                .setParameter("createdAt", LocalDateTime.of(2026, 1, day, 12, 0))
                .setParameter("views", views)
                .setParameter("id", project.getId())
                .executeUpdate();
        entityManager.clear();
        return project.getId();
    }
}
