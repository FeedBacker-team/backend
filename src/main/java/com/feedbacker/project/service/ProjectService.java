package com.feedbacker.project.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.global.common.CustomException;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectSort;
import com.feedbacker.project.domain.ProjectStatus;
import com.feedbacker.project.domain.ProjectTag;
import com.feedbacker.project.domain.dto.request.ProjectCreateRequest;
import com.feedbacker.project.domain.dto.request.ProjectUpdateRequest;
import com.feedbacker.project.domain.dto.response.*;
import com.feedbacker.project.repository.ProjectRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {
    private static final List<FeedbackPostStatus> ACTIVE_QA_STATUSES =
            List.of(
                    FeedbackPostStatus.RECRUITING,
                    FeedbackPostStatus.CLOSED
            );
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final Validator validator;

    //프로젝트 등록
    @Transactional
    public ProjectCreateResponse createProject(UUID memberId, ProjectCreateRequest request) {
        validateLogin(memberId);
        validateRequest(request);
        validateProjectInfo(request.tags(), request.serviceLink(), request.thumbnailImage());

        Member owner = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증된 회원을 찾을 수 없습니다."
                ));

        Project project = Project.create(
                owner,
                request.title(),
                request.description(),
                request.tags(),
                request.serviceLink(),
                request.thumbnailImage()
        );
        Project savedProject = projectRepository.save(project);
        return new ProjectCreateResponse(savedProject.getId());
    }



    //조회수 증가
    @Transactional
    public ProjectDetailResponse getProjectDetail(UUID projectId, UUID viewerId) {
        validateProjectId(projectId);
        int updatedCount = projectRepository.increaseViewCount(
                projectId, ProjectStatus.PUBLISHED
        );

        if (updatedCount == 0) {
            throw projectNotFound();
        }

        Project project = findPublishedProject(projectId);
        return toDetailResponse(project, viewerId);
    }

    //조회수 증가 없는 프로젝트 조회
    public ProjectDetailResponse getProject(UUID projectId, UUID viewerId) {
        Project project = findPublishedProject(projectId);
        return toDetailResponse(project, viewerId);
    }

    // 프로젝트 엔티티 찾기
    public Project getProject(UUID projectId) {
        return projectRepository.findForUpdate(projectId, ProjectStatus.PUBLISHED)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
    }

    public void validateHasFeedbackPost(UUID projectId) {
        if (hasActiveQa(projectId)) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 모집 중인 게시글이 있습니다.");
        }
    }

    //QA 모집 설정에서 선택 가능한 내 프로젝트 목록
    public List<ProjectSummaryResponse> getAvailableProjects(UUID memberId) {
        validateLogin(memberId);

        return projectRepository.findAvailableProjects(
                        memberId,
                        ProjectStatus.PUBLISHED,
                        ACTIVE_QA_STATUSES
                )
                .stream()
                .map(ProjectSummaryResponse::from)
                .toList();
    }

    @Transactional
    public void updateProject(UUID projectId, UUID memberId, ProjectUpdateRequest request) {
        validateLogin(memberId);
        Project project = findProjectForUpdate(projectId);
        validateOwner(project, memberId);
        validateRequest(request);
        validateProjectInfo(
                request.tags(),
                request.serviceLink(),
                request.thumbnailImage()
        );

        project.update(
                request.title(),
                request.description(),
                request.tags(),
                request.serviceLink(),
                request.thumbnailImage()

        );
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID memberId) {
        validateLogin(memberId);
        Project project = findProjectForUpdate(projectId);
        validateOwner(project, memberId);

        if (hasActiveQa(projectId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "진행 중인 QA가 있어 프로젝트를 삭제할 수 없습니다.");
        }

        project.delete();
    }

    public ProjectListResponse getProjects(
            String keyword,
            List<ProjectTag> tags,
            ProjectSort sort,
            int page,
            int size
    ) {
        validateProjectSearch(tags, sort, page, size);

        Specification<Project> specification = hasPublishedStatus();

        if (keyword != null && !keyword.isBlank()) {
            specification = specification.and(containsKeyword(keyword));

        }
        if (tags != null && !tags.isEmpty()) {
            specification = specification.and(hasAnyTag(tags));
        }

        PageRequest pageRequest = PageRequest.of(page, size, createSort(sort));

        Page<ProjectCardResponse> projectPage = projectRepository
                .findAll(specification, pageRequest)
                .map(ProjectCardResponse::from);

        return ProjectListResponse.from(projectPage);
    }

    private void validateProjectSearch(List<ProjectTag> tags, ProjectSort sort, int page, int size) {
        if (tags != null && tags.size() > 5) {
            throw badRequest("태그는 최대 5개까지 선택할 수 있습니다.");
        }
        if (tags != null && tags.stream().distinct().count() != tags.size()) {
            throw badRequest("중복된 태그를 선택할 수 없습니다.");
        }

        if (sort == null) {
            throw badRequest("지원하지 않는 정렬 기준입니다.");
        }

        if (page < 0) {
            throw badRequest("페이지 번호는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > 100) {
            throw badRequest("페이지 크기는 1이상 100 이하여야 합니다.");
        }

    }

    private Specification<Project> hasPublishedStatus() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        ProjectStatus.PUBLISHED
                );
    }

    private Specification<Project> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%"
                    + keyword.trim().toLowerCase(Locale.ROOT)
                    + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("description")),
                            pattern
                    )
            );
        };
    }

    private Specification<Project> hasAnyTag(
            List<ProjectTag> tags
    ) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Join<Project, ProjectTag> tagJoin =
                    root.join("tags", JoinType.INNER);

            return tagJoin.in(tags);
        };
    }

    private Sort createSort(ProjectSort sort) {
        return switch (sort) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );

            case VIEW_COUNT -> Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );
        };
    }

    public List<MyProjectResponse> getMyProjects(UUID memberId) {
        validateLogin(memberId);

        List<Project> projects =
                projectRepository
                        .findByOwner_IdAndStatusOrderByCreatedAtDesc(
                                memberId,
                                ProjectStatus.PUBLISHED
                        );

        List<FeedbackPost> activeQaPosts =
                projectRepository.findActiveQaByOwner(
                        memberId,
                        ProjectStatus.PUBLISHED,
                        ACTIVE_QA_STATUSES
                );

        Map<UUID, UUID> activeQaIdByProjectId =
                new HashMap<>();

        for (FeedbackPost feedbackPost : activeQaPosts) {
            UUID projectId =
                    feedbackPost.getProject().getId();

            activeQaIdByProjectId.putIfAbsent(
                    projectId,
                    feedbackPost.getId()
            );
        }

        return projects.stream()
                .map(project -> MyProjectResponse.from(
                        project,
                        activeQaIdByProjectId.get(project.getId())
                ))
                .toList();
    }

    private boolean hasActiveQa(UUID projectId) {
        validateProjectId(projectId);
        return projectRepository.existsActiveQa(projectId, ACTIVE_QA_STATUSES);
    }

    private void validateOwner(Project project, UUID memberId) {
        if (!project.getOwner().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 소유자만 수정하거나 삭제할 수 있습니다.");
        }

    }

    private Project findProjectForUpdate(UUID projectId) {
        validateProjectId(projectId);
        return projectRepository.findForUpdate(
                projectId,
                ProjectStatus.PUBLISHED
        ).orElseThrow(this::projectNotFound);
    }

    private ProjectDetailResponse toDetailResponse(Project project, UUID viewerId) {
        ActiveQaResponse activeQa = projectRepository.findActiveQa(
                        project.getId(),
                        ACTIVE_QA_STATUSES,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(ActiveQaResponse::from)
                .orElse(null);
        return ProjectDetailResponse.from(
                project,
                viewerId,
                activeQa
        );
    }

    private Project findPublishedProject(UUID projectId) {
        validateProjectId(projectId);
        return projectRepository.findByIdAndStatus(
                projectId,
                ProjectStatus.PUBLISHED
        ).orElseThrow(this::projectNotFound);
    }

    private void validateProjectId(UUID projectId) {
        if (projectId == null) {
            throw badRequest("프로젝트 ID는 필수입니다.");
        }

    }

    private <T> void validateRequest(T request) {
        if (request == null) {
            throw badRequest("요청 데이터는 필수입니다.");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(request);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(violation -> violation.getMessage())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));

            throw badRequest(message);
        }
    }

    private void validateLogin(UUID memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

    }

    private void validateProjectInfo(List<ProjectTag> tags, String serviceLink, String thumbnailImage) {
        if (tags != null && tags.stream().distinct().count() != tags.size()) {
            throw badRequest("중복된 태그를 선택할 수 없습니다.");
        }
        validateHttpUrl(serviceLink, "프로젝트 URL");
        validateHttpUrl(thumbnailImage, "대표 이미지 URL");
    }

    private void validateHttpUrl(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw badRequest(fieldName + "은 필수입니다.");
        }
        try {
            URI uri = new URI(value);
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());

            if (!validScheme
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw badRequest(
                        fieldName
                                + "은 올바른 HTTP 또는 HTTPS 주소여야 합니다."
                );
            }
        } catch (URISyntaxException e) {
            throw badRequest(fieldName + " 형식이 올바르지 않습니다.");
        }

    }

    private ResponseStatusException projectNotFound(){
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
