package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackFormResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackSimpleResponse;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.feedbackpost.service.mapper.FeedbackFormMapper;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberRepository;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectStatus;
import com.feedbacker.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostCardResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostListResponse;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostSort;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FeedbackPostService {

    private final ProjectRepository projectRepository;
    private final FeedbackPostRepository feedbackPostRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final FeedbackFormMapper feedbackFormMapper;
    private final FeedbackRepository feedbackRepository;
    private final ParticipationService participationService;

    @Transactional
    public void create(FeedbackPostCreateRequest request) {

        Project project = projectRepository.findForUpdate(request.projectID(), ProjectStatus.PUBLISHED)
                .orElseThrow(RuntimeException::new);

        UUID memberId = UUID.randomUUID();

        if (project.getOwner().getId() != memberId) {
            throw new RuntimeException();
        }

        if (projectRepository.existsActiveQa(
                project.getId(),
                List.of(FeedbackPostStatus.RECRUITING)
        )) {
            throw new BusinessException(FeedbackPostErrorCode.ACTIVE_FEEDBACK_POST_EXISTS);
        }

        Member member = getMember(memberId);
//
//        if (memberId != 프로젝트 참여 인원) {
//            throw new RuntimeException();
//        }

        // 도토리 차감 검증
        member.getAcornWallet().withdraw(request.depositAcorn());

        feedbackPostRepository.save(request.toEntity(project));
    }

    @Transactional(readOnly = true)
    public FeedbackPostDetailResponse getDetail(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        return FeedbackPostDetailResponse.from(
                feedbackPost,
                imageService.toResponses(feedbackPost.getImages())
        );
    }

    @Transactional(readOnly = true)
    public List<FeedbackSimpleResponse> getMine() {
        UUID memberId = UUID.randomUUID();
        return feedbackPostRepository.findAllByWriterId(memberId).stream()
                .map(post -> FeedbackSimpleResponse.from(
                        post,
                        imageService.toThumbnailResponse(post.getImages())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackFormResponse getForm(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        return feedbackFormMapper.toResponse(feedbackPost.getQuestions());
    }

    @Transactional
    public void participate(UUID feedbackPostId) {
        UUID memberId = UUID.randomUUID();
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        feedbackPost.validateIsWriter(memberId);
        feedbackPost.validateRecruiting();
        participationService.createParticipation(feedbackPost, getMember(memberId));
        feedbackPost.minusRemainSlotCount();
    }

    @Transactional(readOnly = true)
    public List<FeedbackProgressResponse> getFeedbacks(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        UUID memberId = UUID.randomUUID();
        feedbackPost.validateAccessAuth(memberId);
        return feedbackRepository.getAllByFeedbackPost(feedbackPost).stream()
                .map(FeedbackProgressResponse::from)
                .toList();
    }

    @Transactional
    public void complete(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        feedbackPost.complete();
    }

    @Transactional(readOnly = true)
    public FeedbackPostListResponse getFeedbackPosts(
            String keyword,
            List<ProjectTag> tags,
            FeedbackPostSort sort,
            int page,
            int size
    ) {
        validateSearchCondition(tags, sort, page, size);

        Specification<FeedbackPost> specification =
                isCurrentlyRecruiting(LocalDateTime.now());

        if (keyword != null && !keyword.isBlank()) {
            specification = specification.and(
                    containsKeyword(keyword)
            );
        }

        if (tags != null && !tags.isEmpty()) {
            specification = specification.and(
                    hasAnyTag(tags)
            );
        }

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                createSort(sort)
        );

        Page<FeedbackPostCardResponse> feedbackPostPage =
                feedbackPostRepository
                        .findAll(specification, pageRequest)
                        .map(feedbackPost -> {
                            ImageResponse thumbnail =
                                    imageService.toThumbnailResponse(
                                            feedbackPost.getImages()
                                    );

                            String thumbnailUrl =
                                    thumbnail != null
                                            ? thumbnail.url()
                                            : feedbackPost
                                            .getProject()
                                            .getThumbnailImage();

                            return FeedbackPostCardResponse.from(
                                    feedbackPost,
                                    thumbnailUrl
                            );
                        });

        return FeedbackPostListResponse.from(feedbackPostPage);
    }

    private Sort createSort(FeedbackPostSort sort) {
        return switch (sort) {
            case LATEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id")
            );

            case DEADLINE -> Sort.by(
                    Sort.Order.asc("endAt"),
                    Sort.Order.asc("id")
            );
        };
    }

    private Specification<FeedbackPost> hasAnyTag(List<ProjectTag> tags) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Join<Object, Object> projectJoin =
                    root.join("project", JoinType.INNER);

            Join<Object, Object> tagJoin =
                    projectJoin.join("tags", JoinType.INNER);

            return tagJoin.in(tags);
        };
    }

    private Specification<FeedbackPost> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            String pattern =
                    "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            pattern
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    root.get("project").get("title")
                            ),
                            pattern
                    )
            );
        };
    }

    private Specification<FeedbackPost> isCurrentlyRecruiting(LocalDateTime now) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.equal(
                                root.get("status"),
                                FeedbackPostStatus.RECRUITING
                        ),
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("startAt"),
                                now
                        ),
                        criteriaBuilder.greaterThan(
                                root.get("endAt"),
                                now
                        ),
                        criteriaBuilder.greaterThan(
                                root.get("remainSlotCount"),
                                0
                        ),
                        criteriaBuilder.equal(
                                root.get("project").get("status"),
                                ProjectStatus.PUBLISHED
                        )
                );
    }

    private void validateSearchCondition(List<ProjectTag> tags, FeedbackPostSort sort, int page, int size) {
        if (tags != null && tags.size() > 5) {
            throw badRequest(
                    "태그는 최대 5개까지 선택할 수 있습니다."
            );
        }

        if (tags != null
                && tags.stream().distinct().count() != tags.size()) {
            throw badRequest(
                    "중복된 태그를 선택할 수 없습니다."
            );
        }

        if (sort == null) {
            throw badRequest(
                    "지원하지 않는 정렬 기준입니다."
            );
        }

        if (page < 0) {
            throw badRequest(
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size < 1 || size > 100) {
            throw badRequest(
                    "페이지 크기는 1 이상 100 이하여야 합니다."
            );
        }
    }
    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }


    public FeedbackPost getFeedbackPost(UUID feedbackPostId) {
        return feedbackPostRepository.findById(feedbackPostId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND
                ));
    }

    public void validateFeedbackSubmit(FeedbackPost feedbackPost, UUID testerId) {
        feedbackPost.validateRecruiting();
        feedbackPost.validateIsWriter(testerId);
    }

    private Member getMember(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(RuntimeException::new);
    }


}
