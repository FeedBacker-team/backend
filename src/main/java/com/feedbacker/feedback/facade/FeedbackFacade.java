package com.feedbacker.feedback.facade;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackSubmitResponse;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedback.service.QuestionAnswerService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.feedbackpost.service.ParticipationService;
import com.feedbacker.feedbackpost.service.QuestionService;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.AcornWalletService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final AcornWalletService acornWalletService;

    @Transactional
    public FeedbackSubmitResponse submit(UUID memberId, FeedbackSubmitRequest request) {
        Member tester = memberService.getMember(memberId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(request.feedbackPostId());
        List<QuestionAnswer> answers = questionAnswerService.createAnswers(feedbackPost.getId(), request.questionAnswer());
        feedbackPostService.validateFeedbackSubmit(feedbackPost, tester.getId());
        participationService.validateFeedbackSubmit(feedbackPost.getId(), tester.getId());
        return new FeedbackSubmitResponse(feedbackService.submit(
                feedbackPost,
                tester,
                answers
        ));
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getDetail(UUID memberId, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(feedback.getFeedbackPostId());
        Member tester = memberService.getMember(feedback.getTesterId());
        Participation participation = participationService.getParticipation(feedbackPost.getId(), tester.getId());
        QuestionAnswerResponse questionAnswerResponse = questionAnswerService.toResponse(
                questionService.getAllQuestion(feedbackPost.getId()),
                questionAnswerService.getAllQuestionAnswer(feedback.getId())
        );
        feedbackService.validateGetFeedback(memberId, feedback, feedbackPost);
        return FeedbackDetailResponse.from(
                feedbackPost,
                feedback,
                participation,
                questionAnswerResponse
        );
    }

    @Transactional
    public void accept(UUID memberId, UUID feedbackId) {
        // 락 순서: 모집글 -> 피드백 (complete와 동일하게 맞춰 완료 직후 정산을 직렬화)
        UUID feedbackPostId = feedbackService.getFeedback(feedbackId).getFeedbackPostId();
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedbackPost.validateAccessAuth(memberId);
        feedbackPost.validateNotCompleted();
        feedback.accept(feedbackPost.getRewardAcorn());
        payReward(feedback, feedbackPost);
    }

    /** 응답 기한이 지난 제출 피드백을 모집글 단위로 자동 승인 (스케줄러 전용) */
    @Transactional
    public int autoAcceptOverdue(UUID feedbackPostId) {
        // 락 순서: 모집글 -> 피드백 (accept/complete와 동일)
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        // 이미 완료(정산)된 모집글은 환급이 끝났으므로 보상을 추가 지급하지 않는다
        if (feedbackPost.getStatus() == FeedbackPostStatus.COMPLETED) {
            return 0;
        }
        // 락을 잡은 뒤 조회해서, 그 사이 승인/거절된 피드백은 대상에서 빠진다
        List<Feedback> targets = feedbackService.getOverdueFeedbacksForUpdate(feedbackPostId, LocalDateTime.now());
        for (Feedback feedback : targets) {
            feedback.autoAccept(feedbackPost.getRewardAcorn());
            payReward(feedback, feedbackPost);
        }
        return targets.size();
    }

    // 테스터 보상 지급 + 도토리 내역(테스터/작성자) 저장. complete의 환급 계산이 작성자 내역에 의존한다
    private void payReward(Feedback feedback, FeedbackPost feedbackPost) {
        Member tester = memberService.getMember(feedback.getTesterId());
        Member writer = memberService.getMember(feedbackPost.getWriterId());
        acornWalletService.earn(tester.getId(), feedbackPost.getRewardAcorn());
        acornHistoryService.save(
                tester,
                writer,
                feedback,
                feedbackPost
        );
    }

    @Transactional
    public void reject(UUID memberId, FeedbackRejectRequest request, UUID feedbackId) {
        UUID feedbackPostId = feedbackService.getFeedback(feedbackId).getFeedbackPostId();
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostForUpdate(feedbackPostId);
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedbackPost.validateAccessAuth(memberId);
        feedbackPost.validateNotCompleted();
        feedback.reject(request);
        // 이후 거부 사유 검증 로직 추가할 예정
    }

    @Transactional
    public void object(UUID memberId, FeedbackObjectRequest request, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedback.object(memberId, request.objectReason());
        // 이후 어드민에 이의제기 신청 알림 추가할 예정
    }
}