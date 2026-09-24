package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
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

@Service
@RequiredArgsConstructor
public class FeedbackPostService {

    private final ProjectRepository projectRepository;
    private final FeedbackPostRepository feedbackPostRepository;
    private final ParticipationRepository participationRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final FeedbackFormMapper feedbackFormMapper;
    private final FeedbackRepository feedbackRepository;

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
        feedbackPost.validateWriter(memberId);
        feedbackPost.validateRecruiting();

        validateParticipation(
                participationRepository.findForUpdate(feedbackPost.getId()),
                feedbackPost
        );

        Participation reservedParticipation = Participation.reserve(
                feedbackPost,
                getMember(memberId)
        );

        participationRepository.save(reservedParticipation);
        feedbackPost.minusRemainSlotCount();
    }

    @Transactional(readOnly = true)
    public List<FeedbackProgressResponse> getFeedbacks(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        UUID memberId = UUID.randomUUID();
        feedbackPost.validateWriter(memberId);
        return feedbackRepository.getAllByFeedbackPost(feedbackPost).stream()
                .map(FeedbackProgressResponse::from)
                .toList();
    }

    @Transactional
    public void complete(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        feedbackPost.complete();
    }

    private FeedbackPost getFeedbackPost(UUID feedbackPostId) {
        return feedbackPostRepository.findById(feedbackPostId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND
                ));
    }

    private Member getMember(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(RuntimeException::new);
    }

    private void validateParticipation(
            List<Participation> participations,
            FeedbackPost feedbackPost
    ) {
        if (participations.size() > feedbackPost.getSlotCapacity()) {
            throw new BusinessException(FeedbackPostErrorCode.SLOT_FULL);
        }

        for (Participation participation : participations) {
            if (participation.getTester().getId() == feedbackPost.getWriterId()) {
                throw new BusinessException(FeedbackPostErrorCode.ALREADY_PARTICIPATED);
            }
        }
    }
}
