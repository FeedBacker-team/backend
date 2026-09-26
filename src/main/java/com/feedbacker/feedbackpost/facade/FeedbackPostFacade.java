package com.feedbacker.feedbackpost.facade;

import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.feedbackpost.service.ParticipationService;
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
    private final ParticipationService participationService;
    private final FeedbackService feedbackService;

    @Transactional
    public UUID create(CustomUserDetails user, FeedbackPostCreateRequest request) {
        Member member = memberService.getMember(user.getMemberId());
        Project project = projectService.getProject(request.projectId());
        project.validateOwner(member.getId());
        projectService.validateHasFeedbackPost(project.getId());
        acornWalletService.withdraw(member.getId(), request.depositAcorn());
        return feedbackPostService.save(request.toEntity(project));
    }

    @Transactional
    public void participate(CustomUserDetails user, UUID feedbackPostId) {
        Member member = memberService.getMember(user.getMemberId());
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        feedbackPostService.validateParticipation(feedbackPost, member.getId());
        participationService.createParticipation(feedbackPost, member);
        feedbackPost.minusRemainSlotCount();
    }

    @Transactional(readOnly = true)
    public List<FeedbackProgressResponse> getFeedbacks(CustomUserDetails user, UUID feedbackPostId) {
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedbackPostId);
        feedbackPost.validateAccessAuth(user.getMemberId());
        return feedbackService.getAllByFeedbackPost(feedbackPost);
    }
}
