package com.feedbacker.feedback.facade;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackSubmitResponse;
import com.feedbacker.feedback.domain.dto.response.QuestionAnswerResponse;
import com.feedbacker.feedback.service.FeedbackRewardService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedback.service.QuestionAnswerService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.feedbackpost.service.FeedbackPostCompleteService;
import com.feedbacker.feedbackpost.service.QuestionService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackFacade {

    private final MemberService memberService;
    private final FeedbackPostService feedbackPostService;
    private final FeedbackPostCompleteService feedbackPostCompleteService;
    private final FeedbackService feedbackService;
    private final QuestionService questionService;
    private final QuestionAnswerService questionAnswerService;
    private final FeedbackRewardService feedbackRewardService;

    @Transactional
    public FeedbackSubmitResponse submit(UUID memberId, FeedbackSubmitRequest request) {
        Member tester = memberService.getMember(memberId);
        FeedbackPost feedbackPost = getFeedbackPostForSubmit(request.feedbackPostId(), tester.getId());
        List<QuestionAnswer> answers = questionAnswerService.createAnswers(feedbackPost.getId(), request.questionAnswer());
        return new FeedbackSubmitResponse(feedbackService.submit(
                feedbackPost,
                tester,
                answers
        ));
    }

    /** 내 QA 참여 목록: 작성중/포기/만료를 포함한 모든 피드백을 참여 최신순으로 반환한다 */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMine(UUID memberId) {
        return feedbackService.getMyFeedbacks(memberId).stream()
                .sorted(Comparator.comparing(Feedback::getParticipateAt).reversed())
                .map(feedback -> {
                    FeedbackPost feedbackPost = feedback.getFeedbackPost();
                    if (feedbackPost != null && feedbackPost.isDeleted()) {
                        feedbackPost = null;
                    }
                    return FeedbackResponse.from(
                            feedback,
                            feedbackPost,
                            feedbackPostService.getThumbnailUrl(feedbackPost),
                            LocalDateTime.now()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse getDetail(UUID memberId, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedback(feedbackId);
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPost(getFeedbackPostIdOrThrow(feedback));
        QuestionAnswerResponse questionAnswerResponse = questionAnswerService.toResponse(
                questionService.getAllQuestion(feedbackPost.getId()),
                questionAnswerService.getAllQuestionAnswer(feedback.getId())
        );
        feedbackService.validateGetFeedback(memberId, feedback, feedbackPost);
        return FeedbackDetailResponse.from(
                feedbackPost,
                feedback,
                memberService.toImageUrl(feedback.getTesterProfileImage()),
                feedbackPostService.getThumbnail(feedbackPost),
                questionAnswerResponse
        );
    }

    @Transactional
    public void accept(UUID memberId, UUID feedbackId) {
        FeedbackPost feedbackPost = getFeedbackPostForReview(memberId, feedbackId);
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedback.accept(feedbackPost.getRewardAcorn());
        feedbackRewardService.payReward(feedback, feedbackPost);
        feedbackPostCompleteService.completeIfAllReviewed(feedbackPost);
    }

    @Transactional
    public void reject(UUID memberId, FeedbackRejectRequest request, UUID feedbackId) {
        FeedbackPost feedbackPost = getFeedbackPostForReview(memberId, feedbackId);
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedback.reject(request);
        feedbackPostCompleteService.completeIfAllReviewed(feedbackPost);
        // 이후 거부 사유 검증 로직 추가할 예정
    }

    @Transactional
    public void object(UUID memberId, FeedbackObjectRequest request, UUID feedbackId) {
        Feedback feedback = feedbackService.getFeedbackForUpdate(feedbackId);
        feedback.object(memberId, request);
        // 이후 어드민에 이의제기 신청 알림 추가할 예정
    }

    /** 응답 기한이 지난 제출 피드백을 모집글 단위로 자동 승인 (스케줄러 전용) */
    @Transactional
    public int autoAcceptOverdue(UUID feedbackPostId) {
        // 락 순서: 모집글 -> 피드백 (accept/complete와 동일)
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostIncludingDeletedForUpdate(feedbackPostId);
        // 이미 완료(정산)된 모집글은 환급이 끝났으므로 보상을 추가 지급하지 않는다
        if (feedbackPost.getStatus() == FeedbackPostStatus.COMPLETED) {
            return 0;
        }
        // 락을 잡은 뒤 조회해서, 그 사이 승인/거절된 피드백은 대상에서 빠진다
        List<Feedback> targets = feedbackService.getOverdueFeedbacksForUpdate(feedbackPostId, LocalDateTime.now());
        for (Feedback feedback : targets) {
            feedback.autoAccept(feedbackPost.getRewardAcorn());
            feedbackRewardService.payReward(feedback, feedbackPost);
        }
        if (!targets.isEmpty()) {
            feedbackPostCompleteService.completeIfAllReviewed(feedbackPost);
        }
        return targets.size();
    }

    /** 제출 기한이 지난 작성중 피드백을 모집글 단위로 만료시키고 슬롯을 반환 (스케줄러 전용) */
    @Transactional
    public int expireOverdue(UUID feedbackPostId) {
        // 락 순서: 모집글 -> 피드백 (participate/giveUp/submit과 동일)
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostIncludingDeletedForUpdate(feedbackPostId);
        LocalDateTime now = LocalDateTime.now();
        // 락을 잡은 뒤 조회해서, 그 사이 제출/포기된 피드백은 대상에서 빠진다
        List<Feedback> targets = feedbackService.getExpiredFeedbacksForUpdate(feedbackPostId, now);
        for (Feedback feedback : targets) {
            feedback.expire(now);
            feedbackPost.plusRemainSlotCount();
        }
        return targets.size();
    }

    /**
     * 테스터(회원)를 삭제하기 전에 테스터가 참여한 피드백을 정리한다. 회원 삭제와 같은 트랜잭션에서, 회원 삭제보다 먼저 호출한다.
     * - 작성중 피드백은 취소하고 슬롯 반환
     * - 이의제기 중인 피드백은 이의제기를 취소하고 거절로 되돌림 (거절 보상분은 모집글 완료 시 작성자에게 환급된다)
     * - 제출(검토 대기)·승인·거절 피드백은 그대로 둔다. 테스터가 삭제된 뒤 승인되면 보상은 지급되지 않는다
     */
    @Transactional
    public void settleForTesterDelete(UUID testerId) {
        LocalDateTime now = LocalDateTime.now();
        // 여러 모집글에 락을 잡으므로 id 순으로 처리해 락 순서를 고정한다
        for (UUID feedbackPostId : feedbackService.getFeedbackPostIdsToSettleForTesterDelete(testerId)) {
            // 락 순서: 모집글 -> 피드백
            FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostIncludingDeletedForUpdate(feedbackPostId);
            // 락을 잡은 뒤 다시 조회해서, 그 사이 바뀐 상태를 기준으로 처리한다
            Feedback feedback = feedbackService.findMyFeedbackForUpdate(feedbackPostId, testerId);
            if (feedback == null) {
                continue;
            }
            switch (feedback.getStatus()) {
                case WRITING -> {
                    feedback.cancelByForceDelete(now);
                    feedbackPost.plusRemainSlotCount();
                }
                case OBJECTED -> feedback.cancelObjectionByTesterWithdrawal();
                default -> { }
            }
        }
    }

    /** 평가(승인/거절)할 피드백의 모집글을 락 걸어 조회. 작성자 본인이고 완료 전이어야 한다 */
    private FeedbackPost getFeedbackPostForReview(UUID memberId, UUID feedbackId) {
        UUID feedbackPostId = getFeedbackPostIdOrThrow(feedbackService.getFeedback(feedbackId));
        // 락 순서: 모집글 -> 피드백 (complete와 동일하게 맞춰 완료 직후 정산을 직렬화)
        FeedbackPost feedbackPost = feedbackPostService.getFeedbackPostAsWriterForUpdate(feedbackPostId, memberId);
        feedbackPost.validateNotCompleted();
        return feedbackPost;
    }

    /** 피드백의 모집글 id. 모집글이 삭제됐으면 예외 */
    private UUID getFeedbackPostIdOrThrow(Feedback feedback) {
        if (!feedback.hasFeedbackPost()) {
            throw new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND);
        }
        return feedback.getFeedbackPostId();
    }

    private FeedbackPost getFeedbackPostForSubmit(UUID feedbackPostId, UUID testerId) {
        FeedbackPost feedbackPost = feedbackPostService.findFeedbackPostForUpdate(feedbackPostId)
                .orElseThrow(() -> feedbackPostNotFoundForSubmit(feedbackPostId, testerId));
        feedbackPost.validateSubmittable(testerId);
        return feedbackPost;
    }

    /**
     * 제출할 모집글이 없을 때의 예외.
     * 작성 중에 모집글 작성자가 탈퇴해서 모집글이 물리 삭제됐으면, 작성중 피드백은 이미 자동 승인됐으므로 이를 알린다.
     * 모집글이 지워지면 피드백과의 연결(feedback_post_id)이 끊기므로,
     * 모집글 행이 없고 테스터에게 작성자 탈퇴 승인 피드백이 있으면 그 경우로 본다.
     * 락 조회 뒤에 검사해야, 작성자 삭제 트랜잭션을 기다렸다가 조회에 실패한 경우도 잡을 수 있다
     */
    private BusinessException feedbackPostNotFoundForSubmit(UUID feedbackPostId, UUID testerId) {
        if (!feedbackPostService.existsIncludingDeleted(feedbackPostId)
                && feedbackService.hasAcceptedByWriterWithdrawal(testerId)) {
            return new BusinessException(FeedbackErrorCode.FEEDBACK_ACCEPTED_BY_WRITER_WITHDRAWAL);
        }
        return new BusinessException(FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND);
    }

}