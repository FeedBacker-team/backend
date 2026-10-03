package com.feedbacker.feedback.controller;

import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackSubmitResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.facade.FeedbackFacade;
import com.feedbacker.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackFacade feedbackFacade;

    @PostMapping
    public FeedbackSubmitResponse submit(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackSubmitRequest request
    ) {
        return feedbackFacade.submit(user.getMemberId(), request);
    }

    @GetMapping("/mine")
    public List<FeedbackResponse> getMine(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return feedbackFacade.getMine(user.getMemberId());
    }

    @GetMapping("/{feedbackId}")
    public FeedbackDetailResponse getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackId
    ) {
        return feedbackFacade.getDetail(user.getMemberId(), feedbackId);
    }

    @PatchMapping("/{feedbackId}/accept")
    public void accept(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.accept(user.getMemberId(), feedbackId);
    }

    @PatchMapping("/{feedbackId}/reject")
    public void reject(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackRejectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.reject(user.getMemberId(), request, feedbackId);
    }

    @PatchMapping("/{feedbackId}/objection")
    public void object(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackObjectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.object(user.getMemberId(), request, feedbackId);
    }

}
