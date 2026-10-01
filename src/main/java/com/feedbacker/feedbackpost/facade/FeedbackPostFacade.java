package com.feedbacker.feedbackpost.facade;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.response.AcornHistoryResponse;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.domain.type.FeedbackStatus;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedback.service.QuestionAnswerService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostResultResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackResultResponse;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.feedbackpost.service.ImageService;
import com.feedbacker.feedbackpost.service.ParticipationService;
import com.feedbacker.feedbackpost.service.QuestionService;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.AcornWalletService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackPostFacade {

    private final FeedbackPostService feedbackPostService;
    private final ProjectService projectService;
    private final MemberService memberService;
    private final AcornWalletService acornWalletService;
    private final AcornHistoryService acornHistoryService;
    private final ParticipationService participationService;
    private final FeedbackService feedbackService;
    private final ImageService imageService;
    private final QuestionAnswerService questionAnswerService;
    private final QuestionService questionService;

    @Transactional
    public UUID create(UUID memberId, FeedbackPostCreateRequest request) {
        Member member = memberService.getMember(memberId);
        Project project = projectService.getProject(request.projectId());
        project.validateOwner(member.getId());
        projectService.validateHasFeedbackPost(project.getId());
        acornWalletService.withdraw(member.getId(), request.depositAcorn());
        return feedbackPostService.save(request.toEntity(project));
    }

    @Transactional(readOnly = true)
    public FeedbackPostDetailResponse getDetail(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        Feedback feedback = memberId == null ? null : feedbackService.findMyFeedback(feedbackPostId, memberId);

        // 수정 예정
        FeedbackStatus feedbackStatus = null;
        if (participationService.isWriting(feedbackPost.getId(), memberId)) {
            feedbackStatus = FeedbackStatus.WRITING;
        }

        return FeedbackPostDetailResponse.from(
                feedback,
                feedbackPost,
                imageService.toResponses(feedbackPost.getImages()),
                feedbackStatus
        );
    }

    @Transactional
    public void participate(UUID memberId, UUID feedbackPostId) {
        Member member = memberService.getMember(memberId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        feedbackPostService.validateParticipation(feedbackPost, member.getId());
        participationService.validateAlreadyParticipate(feedbackPostId, memberId);
        participationService.createParticipation(feedbackPost, member);
        feedbackPost.minusRemainSlotCount();
    }

    @Transactional(readOnly = true)
    public List<FeedbackProgressResponse> getFeedbacks(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        feedbackPost.validateAccessAuth(memberId);
        return feedbackService.getFeedbackProgress(feedbackPost.getId());
    }

    @Transactional
    public void complete(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        participationService.validateNoActiveParticipant(feedbackPost.getId());
        feedbackPost.complete(memberId);
        int paidAcorn = acornHistoryService.combine(memberId, feedbackPost.getId());
        int refundAcorn = feedbackPost.getDepositAcorn() - paidAcorn;
        if (refundAcorn > 0) {
            acornWalletService.earn(memberId, refundAcorn);
        }
    }

    @Transactional(readOnly = true)
    public FeedbackPostResultResponse getResult(UUID memberId, UUID feedbackPostId) {
        List<Feedback> feedbacks = feedbackService.getAllFeedbacks(feedbackPostId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        feedbackPost.validateAccessAuth(memberId);
        feedbackPost.validateCompleted();
        List<ImageResponse> images = imageService.toResponses(feedbackPost.getImages());
        List<FeedbackResultResponse> feedbackResults = questionAnswerService.getFeedbackResults(feedbacks, feedbackPost);
        AcornHistoryResponse acornHistory = acornHistoryService.getAcronHistoryByFeedbackPost(
                memberId,
                feedbackPost.getId()
        );
        return FeedbackPostResultResponse.from(
                feedbackResults,
                feedbackPost,
                acornHistory,
                images
        );
    }
}
