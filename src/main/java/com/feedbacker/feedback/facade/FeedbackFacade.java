package com.feedbacker.feedback.facade;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedback.service.QuestionAnswerService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.feedbackpost.service.ParticipationService;
import com.feedbacker.feedbackpost.service.QuestionService;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackFacade {

    private final MemberService memberService;
    private final ParticipationService participationService;
    private final FeedbackPostService feedbackPostService;
    private final FeedbackService feedbackService;
    private final QuestionService questionService;
    private final QuestionAnswerService questionAnswerService;
    private final AcornHistoryService acornHistoryService;

    @Transactional
    public UUID submit(CustomUserDetails user, FeedbackSubmitRequest request) {
        Member tester = memberService.getMember(user.getMemberId());
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(request.feedbackPostId());
        List<QuestionAnswer> answers = questionAnswerService.createAnswers(request.questionAnswer());
        feedbackPostService.validateFeedbackSubmit(feedbackPost, tester.getId());
        participationService.validateFeedbackSubmit(feedbackPost.getId(), tester.getId());
        return feedbackService.submit(
                feedbackPost,
                tester,
                answers
        );
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getDetail(CustomUserDetails user, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedback.getFeedbackPostId());
        Member tester = memberService.getMember(feedback.getTesterId());
        Participation participation = participationService.getParticipation(tester);
        QuestionAnswerResponse questionAnswerResponse = questionAnswerService.toResponse(
                questionService.getAllQuestion(feedbackPost.getId()),
                questionAnswerService.getAllQuestionAnswer(feedback.getId())
        );
        feedbackService.validateGetFeedback(user.getMemberId(), feedback, feedbackPost);
        return FeedbackDetailResponse.from(
                feedbackPost,
                feedback,
                participation,
                questionAnswerResponse
        );
    }

    @Transactional
    public void accept(CustomUserDetails user, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedback.getFeedbackPostId());
        feedbackPost.validateAccessAuth(user.getMemberId());
        Member tester = memberService.getMember(feedback.getTesterId());
        Member writer = memberService.getMember(feedbackPost.getWriterId());
        feedback.accept(feedbackPost.getRewardAcorn());
        acornHistoryService.save(
                tester,
                writer,
                feedback,
                feedbackPost
        );
    }

    @Transactional
    public void reject(CustomUserDetails user, FeedbackRejectRequest request, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedback.getFeedbackPostId());
        feedbackPost.validateAccessAuth(user.getMemberId());
        feedback.reject(request);
        // 이후 거부 사유 검증 로직 추가할 예정
    }

    @Transactional
    public void object(CustomUserDetails user, FeedbackObjectRequest request, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedback.getFeedbackPostId());
        feedbackPost.validateAccessAuth(user.getMemberId());
        feedback.setObjectReason(request.objectReason());
        // 이후 어드민에 이의제기 신청 알림 추가할 예정
    }
}
