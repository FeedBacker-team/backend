package com.feedbacker.feedbackpost.facade;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedback.service.QuestionAnswerService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostCreateResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostResultResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackResultResponse;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    public FeedbackPostCreateResponse create(UUID memberId, FeedbackPostCreateRequest request) {
        Member member = memberService.getMember(memberId);
        Project project = projectService.getProject(request.projectId());
        project.validateOwner(member.getId());
        projectService.validateHasFeedbackPost(project.getId());
        acornWalletService.withdraw(member.getId(), request.depositAcorn());
        UUID feedbackPostId = feedbackPostService.save(request.toEntity(project));
        acornHistoryService.saveDeposit(member, feedbackPostId, request.depositAcorn());
        return new FeedbackPostCreateResponse(feedbackPostId);
    }

    @Transactional(readOnly = true)
    public FeedbackPostDetailResponse getDetail(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        Feedback feedback = memberId == null ? null : feedbackService.findMyFeedback(feedbackPostId, memberId);

        Participation participation = memberId == null ? null
                : participationService.findParticipation(feedbackPostId, memberId).orElse(null);

        return FeedbackPostDetailResponse.from(
                feedback,
                participation,
                feedbackPost,
                imageService.toResponses(feedbackPost.getImages())
        );
    }

    @Transactional
    public void participate(UUID memberId, UUID feedbackPostId) {
        Member member = memberService.getMember(memberId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        feedbackPostService.validateParticipation(feedbackPost, member.getId());
        feedbackPost.minusRemainSlotCount();
        feedbackService.participate(feedbackPost, member);
    }

    @Transactional
    public void giveUp(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        feedbackService.giveUp(feedbackPost.getId(), memberId);
        feedbackPost.plusRemainSlotCount();
    }

    @Transactional(readOnly = true)
    public List<FeedbackProgressResponse> getFeedbacks(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        feedbackPost.validateAccessAuth(memberId);
        List<Feedback> feedbacks = feedbackService.getAllFeedbacks(feedbackPost.getId());
        Map<UUID, String> profileImageUrls = memberService.getProfileImageUrls(
                feedbacks.stream().map(feedback -> feedback.getTester().getId()).toList()
        );
        return feedbacks.stream()
                .map(feedback -> FeedbackProgressResponse.from(
                        feedback,
                        profileImageUrls.get(feedback.getTester().getId())
                ))
                .toList();
    }

    @Transactional
    public void complete(UUID memberId, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        feedbackPost.validateAccessAuth(memberId);
        feedbackService.validateExistActive(feedbackPost.getId());
        settle(feedbackPost);
    }

    /** 모집 기간이 끝난 모집글을 자동 완료·환급 (스케줄러 전용). 완료 처리했으면 true */
    @Transactional
    public boolean autoCompleteEnded(UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        // 락을 잡은 뒤 다시 확인해서, 그 사이 작성자가 직접 완료한 모집글은 건너뛴다
        if (feedbackPost.getStatus() == FeedbackPostStatus.COMPLETED
                || LocalDateTime.now().isBefore(feedbackPost.getEndAt())) {
            return false;
        }
        // 작성중인 참여·검토 대기 피드백이 남아 있으면 만료/자동 승인된 뒤 다음 주기에 완료한다
        if (feedbackService.hasWritingFeedback(feedbackPost.getId())
                || feedbackService.hasSubmittedFeedback(feedbackPost.getId())) {
            return false;
        }
        settle(feedbackPost);
        return true;
    }

    // 모집글 완료 처리 + 작성자에게 남은 예치 도토리 환급
    private void settle(FeedbackPost feedbackPost) {
        UUID writerId = feedbackPost.getWriterId();
        feedbackPost.complete(writerId);
        // 환급 = 예치 - (승인 수 * 건당 보상)
        int refundAcorn = feedbackPost.getDepositAcorn() - getPaidAcorn(feedbackPost);
        if (refundAcorn > 0) {
            acornWalletService.earn(writerId, refundAcorn);
            acornHistoryService.saveRefund(memberService.getMember(writerId), feedbackPost.getId(), refundAcorn);
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
        return FeedbackPostResultResponse.from(
                feedbackResults,
                feedbackPost,
                getPaidAcorn(feedbackPost),
                images
        );
    }

    // 승인된 피드백에 지급된 도토리 합계
    private int getPaidAcorn(FeedbackPost feedbackPost) {
        return feedbackService.countAcceptedFeedbacks(feedbackPost.getId()) * feedbackPost.getRewardAcorn();
    }
}
