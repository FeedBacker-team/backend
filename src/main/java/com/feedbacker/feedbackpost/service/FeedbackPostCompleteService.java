package com.feedbacker.feedbackpost.service;

import com.feedbacker.acorn.service.AcornHistoryService;
import com.feedbacker.feedback.service.FeedbackService;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.member.AcornWalletService;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackPostCompleteService {

    private final FeedbackService feedbackService;
    private final MemberService memberService;
    private final AcornWalletService acornWalletService;
    private final AcornHistoryService acornHistoryService;

    /** 모집글 완료 처리 + 작성자에게 남은 예치 도토리 환급. 모집글 락을 잡은 트랜잭션 안에서 호출한다 */
    public void complete(FeedbackPost feedbackPost) {
        UUID writerId = feedbackPost.getWriterId();
        feedbackPost.complete();
        // 환급 = 예치 - (승인 수 * 건당 보상)
        int refundAcorn = feedbackPost.getDepositAcorn() - getPaidAcorn(feedbackPost);
        if (refundAcorn > 0) {
            acornWalletService.earn(writerId, refundAcorn);
            acornHistoryService.recordRefund(memberService.getMember(writerId), feedbackPost.getId(), refundAcorn);
        }
    }

    /**
     * 슬롯이 모두 찼고 작성중·검토 대기 피드백이 없으면 자동 완료·환급 (승인/거절 직후 사용). 완료 처리했으면 true
     * 거절된 피드백의 보상분은 이의제기 결과와 상관없이 이 시점에 작성자에게 환급된다
     */
    public boolean completeIfAllReviewed(FeedbackPost feedbackPost) {
        if (feedbackPost.getRemainSlotCount() > 0) {
            return false;
        }
        if (feedbackService.hasPendingFeedback(feedbackPost.getId())) {
            return false;
        }
        complete(feedbackPost);
        return true;
    }

    // 승인된 피드백에 지급된 도토리 합계
    public int getPaidAcorn(FeedbackPost feedbackPost) {
        return feedbackService.countAcceptedFeedbacks(feedbackPost.getId()) * feedbackPost.getRewardAcorn();
    }
}
