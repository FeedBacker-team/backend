package com.feedbacker.project.service;

import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.global.storage.SupabaseStorageService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectStatus;
import com.feedbacker.project.domain.ProjectTag;
import com.feedbacker.project.domain.dto.request.ProjectCreateRequest;
import com.feedbacker.project.domain.dto.request.ProjectUpdateRequest;
import com.feedbacker.project.domain.dto.response.ProjectCreateResponse;
import com.feedbacker.project.domain.dto.response.ProjectDetailResponse;
import com.feedbacker.project.domain.dto.response.ProjectSummaryResponse;
import com.feedbacker.project.repository.ProjectRepository;
import com.feedbacker.project.repository.ProjectViewRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProjectViewRepository projectViewRepository;

    private ProjectService projectService;

    private Validator validator;

    private static final String THUMBNAIL_PATH =
            "images/550e8400-e29b-41d4-a716-446655440000.png";

    private static final String THUMBNAIL_URL =
            "https://feedbacker-storage.supabase.co/storage/v1/object/public/images/"
                    + THUMBNAIL_PATH;

    @Mock
    private SupabaseStorageService storageService;

    @BeforeEach
    void setUp() {
        validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();

        projectService = new ProjectService(
                projectRepository,
                memberRepository,
                validator,
                storageService,
                projectViewRepository
        );
    }

    @Nested
    @DisplayName("프로젝트 등록")
    class CreateProject {

        @Test
        @DisplayName("정상적인 요청이면 프로젝트를 등록하고 ID를 반환한다")
        void createProjectSuccess() {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project savedProject = mock(Project.class);

            ProjectCreateRequest request =
                    createValidCreateRequest();

            when(memberRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(savedProject.getId()).thenReturn(projectId);
            when(projectRepository.save(any(Project.class)))
                    .thenReturn(savedProject);

            // when
            ProjectCreateResponse response =
                    projectService.createProject(ownerId, request);

            // then
            assertThat(response.projectId())
                    .isEqualTo(projectId);

            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("프로젝트 제목이 비어 있으면 400 예외가 발생한다")
        void createProjectWithBlankTitle() {
            // given
            UUID ownerId = UUID.randomUUID();

            ProjectCreateRequest request =
                    new ProjectCreateRequest(
                            "",
                            "프로젝트 설명",
                            List.of(ProjectTag.WEB),
                            "https://example.com",
                            THUMBNAIL_PATH
                    );

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.createProject(ownerId, request)
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(projectRepository, never())
                    .save(any(Project.class));
        }

        @Test
        @DisplayName("로그인 사용자가 없으면 401 예외가 발생한다")
        void createProjectWithoutLogin() {
            // given
            ProjectCreateRequest request =
                    createValidCreateRequest();

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.createProject(null, request)
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verify(projectRepository, never())
                    .save(any(Project.class));
        }

        @Test
        @DisplayName("중복 태그가 있으면 400 예외가 발생한다")
        void createProjectWithDuplicatedTags() {
            // given
            UUID ownerId = UUID.randomUUID();

            ProjectCreateRequest request =
                    new ProjectCreateRequest(
                            "프로젝트 제목",
                            "프로젝트 설명",
                            List.of(ProjectTag.WEB, ProjectTag.WEB),
                            "https://example.com",
                            THUMBNAIL_PATH
                    );

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.createProject(ownerId, request)
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(projectRepository, never())
                    .save(any(Project.class));
        }
    }

    @Nested
    @DisplayName("프로젝트 상세 조회")
    class GetProjectDetail {

        @Test
        @DisplayName("작성자도 최초 로그인 조회 시 한 번 증가시키고 최신 상세 응답을 반환한다")
        void getProjectDetailSuccess() {
            // given
            UUID projectId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project project = mock(Project.class);
            Project beforeIncrease = mock(Project.class);

            when(owner.getId()).thenReturn(ownerId);
            when(owner.getNickname()).thenReturn("메이커");
            when(project.getId()).thenReturn(projectId);
            when(project.getOwner()).thenReturn(owner);
            when(project.getTitle()).thenReturn("프로젝트 제목");
            when(project.getDescription()).thenReturn("프로젝트 설명");
            when(project.getTags())
                    .thenReturn(List.of(ProjectTag.WEB));
            when(project.getThumbnailImage()).thenReturn(THUMBNAIL_PATH);
            when(project.getViewCount()).thenReturn(11L);
            when(storageService.createPublicUrl(THUMBNAIL_PATH))
                    .thenReturn(THUMBNAIL_URL);

            when(memberRepository.existsById(ownerId)).thenReturn(true);
            when(projectViewRepository.insertIfAbsent(projectId, ownerId)).thenReturn(1);

            when(projectRepository.increaseViewCount(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(1);

            when(projectRepository.findByIdAndStatus(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.of(beforeIncrease), Optional.of(project));

            when(projectRepository.findActiveQa(
                    eq(projectId),
                    anyCollection(),
                    any(Pageable.class)
            )).thenReturn(List.of());

            // when
            ProjectDetailResponse response =
                    projectService.getProjectDetail(
                            projectId,
                            ownerId
                    );

            // then
            assertThat(response.projectId())
                    .isEqualTo(projectId);

            assertThat(response.isOwner()).isTrue();
            assertThat(response.hasActiveQa()).isFalse();
            assertThat(response.activeQa()).isNull();
            assertThat(response.thumbnailImage()).isEqualTo(THUMBNAIL_URL);
            assertThat(response.viewCount()).isEqualTo(11L);

            var order = inOrder(projectViewRepository, projectRepository);
            order.verify(projectViewRepository).insertIfAbsent(projectId, ownerId);
            order.verify(projectRepository).increaseViewCount(projectId, ProjectStatus.PUBLISHED);
            order.verify(projectRepository).findByIdAndStatus(projectId, ProjectStatus.PUBLISHED);

            verify(projectRepository).increaseViewCount(
                    projectId,
                    ProjectStatus.PUBLISHED
            );

            verify(projectRepository, times(2)).findByIdAndStatus(
                    projectId,
                    ProjectStatus.PUBLISHED
            );

            verify(projectRepository, never())
                    .findForUpdate(any(), any());
            verify(storageService).createPublicUrl(THUMBNAIL_PATH);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트이면 404 예외가 발생한다")
        void getProjectDetailNotFound() {
            // given
            UUID projectId = UUID.randomUUID();

            when(projectRepository.findByIdAndStatus(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.empty());

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.getProjectDetail(
                            projectId,
                            null
                    )
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(projectRepository, never()).increaseViewCount(any(), any());
            verifyNoInteractions(memberRepository, projectViewRepository);
        }

        @Test
        @DisplayName("비로그인 조회는 기존 조회수를 반환하고 회원 및 조회 이력에 접근하지 않는다")
        void anonymousViewDoesNotIncreaseCount() {
            UUID projectId = UUID.randomUUID();
            stubDetail(projectId, 10L);

            ProjectDetailResponse response = projectService.getProjectDetail(projectId, null);

            assertThat(response.viewCount()).isEqualTo(10L);
            assertThat(response.isOwner()).isFalse();
            verifyNoInteractions(memberRepository, projectViewRepository);
            verify(projectRepository, never()).increaseViewCount(any(), any());
            verify(projectRepository).findByIdAndStatus(projectId, ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("이미 조회한 회원은 조회수를 증가시키지 않는다")
        void existingViewerDoesNotIncreaseCount() {
            UUID projectId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            stubDetail(projectId, 10L);
            when(memberRepository.existsById(viewerId)).thenReturn(true);
            when(projectViewRepository.insertIfAbsent(projectId, viewerId)).thenReturn(0);

            ProjectDetailResponse response = projectService.getProjectDetail(projectId, viewerId);

            assertThat(response.viewCount()).isEqualTo(10L);
            verify(projectViewRepository).insertIfAbsent(projectId, viewerId);
            verify(projectRepository, never()).increaseViewCount(any(), any());
            verify(projectRepository).findByIdAndStatus(projectId, ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("같은 회원이 연속 조회하면 신규 이력일 때만 한 번 증가한다")
        void repeatedViewsIncreaseOnlyOnce() {
            UUID projectId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            stubDetail(projectId, 11L);
            when(memberRepository.existsById(viewerId)).thenReturn(true);
            when(projectViewRepository.insertIfAbsent(projectId, viewerId)).thenReturn(1, 0);
            when(projectRepository.increaseViewCount(projectId, ProjectStatus.PUBLISHED)).thenReturn(1);

            assertThat(projectService.getProjectDetail(projectId, viewerId).viewCount()).isEqualTo(11L);
            assertThat(projectService.getProjectDetail(projectId, viewerId).viewCount()).isEqualTo(11L);

            verify(projectViewRepository, times(2)).insertIfAbsent(projectId, viewerId);
            verify(projectRepository).increaseViewCount(projectId, ProjectStatus.PUBLISHED);
            verify(projectRepository, times(3)).findByIdAndStatus(projectId, ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("서로 다른 회원의 최초 조회는 각각 증가한다")
        void distinctViewersEachIncreaseCount() {
            UUID projectId = UUID.randomUUID();
            UUID firstViewer = UUID.randomUUID();
            UUID secondViewer = UUID.randomUUID();
            Project project = stubDetail(projectId, 11L);
            when(project.getViewCount()).thenReturn(11L, 12L);
            when(memberRepository.existsById(firstViewer)).thenReturn(true);
            when(memberRepository.existsById(secondViewer)).thenReturn(true);
            when(projectViewRepository.insertIfAbsent(projectId, firstViewer)).thenReturn(1);
            when(projectViewRepository.insertIfAbsent(projectId, secondViewer)).thenReturn(1);
            when(projectRepository.increaseViewCount(projectId, ProjectStatus.PUBLISHED)).thenReturn(1);

            assertThat(projectService.getProjectDetail(projectId, firstViewer).viewCount()).isEqualTo(11L);
            assertThat(projectService.getProjectDetail(projectId, secondViewer).viewCount()).isEqualTo(12L);

            verify(projectViewRepository).insertIfAbsent(projectId, firstViewer);
            verify(projectViewRepository).insertIfAbsent(projectId, secondViewer);
            verify(projectRepository, times(2)).increaseViewCount(projectId, ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 401이며 조회 이력을 저장하지 않는다")
        void missingMemberDoesNotRecordView() {
            UUID projectId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            when(projectRepository.findByIdAndStatus(projectId, ProjectStatus.PUBLISHED))
                    .thenReturn(Optional.of(mock(Project.class)));
            when(memberRepository.existsById(viewerId)).thenReturn(false);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> projectService.getProjectDetail(projectId, viewerId));

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verifyNoInteractions(projectViewRepository);
            verify(projectRepository, never()).increaseViewCount(any(), any());
        }

        @Test
        @DisplayName("조회수 갱신 대상이 사라지면 404 예외를 전파한다")
        void failedIncreaseThrowsNotFound() {
            UUID projectId = UUID.randomUUID();
            UUID viewerId = UUID.randomUUID();
            when(projectRepository.findByIdAndStatus(projectId, ProjectStatus.PUBLISHED))
                    .thenReturn(Optional.of(mock(Project.class)));
            when(memberRepository.existsById(viewerId)).thenReturn(true);
            when(projectViewRepository.insertIfAbsent(projectId, viewerId)).thenReturn(1);
            when(projectRepository.increaseViewCount(projectId, ProjectStatus.PUBLISHED)).thenReturn(0);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> projectService.getProjectDetail(projectId, viewerId));

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(projectRepository).findByIdAndStatus(projectId, ProjectStatus.PUBLISHED);
            verify(projectRepository, never()).findActiveQa(any(), anyCollection(), any(Pageable.class));
        }

        @Test
        @DisplayName("프로젝트 ID가 없으면 400이며 DB에 접근하지 않는다")
        void missingProjectIdThrowsBadRequest() {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> projectService.getProjectDetail(null, UUID.randomUUID()));

            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(projectRepository, memberRepository, projectViewRepository);
        }

        private Project stubDetail(UUID projectId, long viewCount) {
            Member owner = mock(Member.class);
            Project project = mock(Project.class);
            when(owner.getId()).thenReturn(UUID.randomUUID());
            when(project.getOwner()).thenReturn(owner);
            when(project.getId()).thenReturn(projectId);
            when(project.getViewCount()).thenReturn(viewCount);
            when(projectRepository.findByIdAndStatus(projectId, ProjectStatus.PUBLISHED))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findActiveQa(eq(projectId), anyCollection(), any(Pageable.class)))
                    .thenReturn(List.of());
            return project;
        }
    }

    @Nested
    @DisplayName("QA 모집에 사용할 프로젝트 목록")
    class GetAvailableProjects {

        @Test
        @DisplayName("활성 QA가 없는 내 프로젝트 목록을 반환한다")
        void getAvailableProjectsSuccess() {
            // given
            UUID memberId = UUID.randomUUID();
            UUID projectId = UUID.randomUUID();

            Project project = mock(Project.class);

            when(project.getId()).thenReturn(projectId);
            when(project.getTitle()).thenReturn("프로젝트 제목");
            when(project.getDescription())
                    .thenReturn("프로젝트 설명");

            when(projectRepository.findAvailableProjects(
                    eq(memberId),
                    eq(ProjectStatus.PUBLISHED),
                    anyCollection()
            )).thenReturn(List.of(project));

            // when
            List<ProjectSummaryResponse> responses =
                    projectService.getAvailableProjects(memberId);

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().projectId())
                    .isEqualTo(projectId);
            assertThat(responses.getFirst().title())
                    .isEqualTo("프로젝트 제목");
        }

        @Test
        @DisplayName("선택 가능한 프로젝트가 없으면 빈 목록을 반환한다")
        void getAvailableProjectsEmpty() {
            // given
            UUID memberId = UUID.randomUUID();

            when(projectRepository.findAvailableProjects(
                    eq(memberId),
                    eq(ProjectStatus.PUBLISHED),
                    anyCollection()
            )).thenReturn(List.of());

            // when
            List<ProjectSummaryResponse> responses =
                    projectService.getAvailableProjects(memberId);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("프로젝트 수정")
    class UpdateProject {

        @Test
        @DisplayName("프로젝트 소유자는 프로젝트를 수정할 수 있다")
        void updateProjectSuccess() {
            // given
            UUID projectId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project project = mock(Project.class);

            when(owner.getId()).thenReturn(memberId);
            when(project.getOwner()).thenReturn(owner);

            when(projectRepository.findForUpdate(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.of(project));

            ProjectUpdateRequest request =
                    createValidUpdateRequest();

            // when
            projectService.updateProject(
                    projectId,
                    memberId,
                    request
            );

            // then
            verify(project).update(
                    request.title(),
                    request.description(),
                    request.tags(),
                    request.serviceLink(),
                    request.thumbnailImage()
            );
        }

        @Test
        @DisplayName("프로젝트 소유자가 아니면 403 예외가 발생한다")
        void updateProjectForbidden() {
            // given
            UUID projectId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            UUID otherMemberId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project project = mock(Project.class);

            when(owner.getId()).thenReturn(ownerId);
            when(project.getOwner()).thenReturn(owner);

            when(projectRepository.findForUpdate(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.of(project));

            ProjectUpdateRequest request =
                    createValidUpdateRequest();

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.updateProject(
                            projectId,
                            otherMemberId,
                            request
                    )
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(project, never()).update(
                    anyString(),
                    anyString(),
                    anyList(),
                    anyString(),
                    anyString()
            );
        }
    }

    @Nested
    @DisplayName("프로젝트 삭제")
    class DeleteProject {

        @Test
        @DisplayName("활성 QA가 없으면 프로젝트를 삭제한다")
        void deleteProjectSuccess() {
            // given
            UUID projectId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project project = mock(Project.class);

            when(owner.getId()).thenReturn(memberId);
            when(project.getOwner()).thenReturn(owner);

            when(projectRepository.findForUpdate(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.of(project));

            when(projectRepository.existsActiveQa(
                    eq(projectId),
                    anyCollection()
            )).thenReturn(false);

            // when
            projectService.deleteProject(
                    projectId,
                    memberId
            );

            // then
            verify(project).delete();
        }

        @Test
        @DisplayName("활성 QA가 있으면 409 예외가 발생한다")
        void deleteProjectWithActiveQa() {
            // given
            UUID projectId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();

            Member owner = mock(Member.class);
            Project project = mock(Project.class);

            when(owner.getId()).thenReturn(memberId);
            when(project.getOwner()).thenReturn(owner);

            when(projectRepository.findForUpdate(
                    projectId,
                    ProjectStatus.PUBLISHED
            )).thenReturn(Optional.of(project));

            when(projectRepository.existsActiveQa(
                    eq(projectId),
                    anyCollection()
            )).thenReturn(true);

            // when
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> projectService.deleteProject(
                            projectId,
                            memberId
                    )
            );

            // then
            assertThat(exception.getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(project, never()).delete();
        }
    }

    private ProjectCreateRequest createValidCreateRequest() {
        return new ProjectCreateRequest(
                "피드배커 온보딩 리디자인",
                "온보딩 흐름 사용성 개선 프로젝트입니다.",
                List.of(ProjectTag.WEB, ProjectTag.UX),
                "https://example.com",
                THUMBNAIL_PATH
        );
    }

    private ProjectUpdateRequest createValidUpdateRequest() {
        return new ProjectUpdateRequest(
                "수정된 프로젝트 제목",
                "수정된 프로젝트 설명",
                List.of(ProjectTag.WEB, ProjectTag.B2B),
                "https://example.com/updated",
                THUMBNAIL_PATH
        );
    }
}
