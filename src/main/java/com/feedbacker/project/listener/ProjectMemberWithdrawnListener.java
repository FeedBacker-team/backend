package com.feedbacker.project.listener;

import com.feedbacker.member.MemberWithdrawnEvent;
import com.feedbacker.project.service.ProjectDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 회원 탈퇴 시 프로젝트 정리. */
@Component
@RequiredArgsConstructor
public class ProjectMemberWithdrawnListener {

    private final ProjectDeleteService projectDeleteService;

    @Order(2)
    @EventListener
    public void handle(MemberWithdrawnEvent event) {
        projectDeleteService.deleteAllByOwner(event.memberId());
    }
}
