package com.feedbacker.feedback.listener;

import com.feedbacker.feedback.facade.FeedbackFacade;
import com.feedbacker.feedbackpost.service.FeedbackPostDeleteService;
import com.feedbacker.member.MemberWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 회원 탈퇴 시 피드백·모집글 정리. */
@Component
@RequiredArgsConstructor
public class FeedbackMemberWithdrawnListener {

    private final FeedbackFacade feedbackFacade;
    private final FeedbackPostDeleteService feedbackPostDeleteService;

    @Order(1)
    @EventListener
    public void handle(MemberWithdrawnEvent event) {
        // 테스터로 참여한 다른 회원의 모집글 피드백 정리 (작성중 취소, 이의제기 취소)
        feedbackFacade.settleForTesterDelete(event.memberId());
        // 내가 작성한 모집글 정산 후 물리 삭제 (작성중 피드백은 작성자 탈퇴 승인)
        feedbackPostDeleteService.deleteAllByWriter(event.memberId());
    }
}
