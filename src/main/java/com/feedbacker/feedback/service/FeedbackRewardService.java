package com.feedbacker.feedback.service;

import com.feedbacker.acorn.service.AcornHistoryService;
import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.member.AcornWalletService;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackRewardService {

    private final MemberService memberService;
    private final AcornWalletService acornWalletService;
    private final AcornHistoryService acornHistoryService;

    /** 테스터 보상 지급 + 테스터 도토리 내역 저장. 작성자는 등록 시 예치 내역으로 이미 차감이 기록되어 있다 */
    public void payReward(Feedback feedback, FeedbackPost feedbackPost) {
        // 테스터(회원)가 삭제된 피드백은 지급 대상이 없다
        if (!feedback.hasTester()) {
            return;
        }
        Member tester = memberService.getMember(feedback.getTesterId());
        acornWalletService.earn(tester.getId(), feedbackPost.getRewardAcorn());
        acornHistoryService.recordReward(tester, feedback, feedbackPost);
    }
}
