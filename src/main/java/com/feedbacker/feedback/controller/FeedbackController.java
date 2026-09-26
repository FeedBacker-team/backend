package com.feedbacker.feedback.controller;

import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.facade.FeedbackFacade;
import com.feedbacker.feedback.service.FeedbackService;
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
    private final FeedbackService feedbackService;

    @PostMapping
    public UUID submit(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackSubmitRequest request
    ) {
        return feedbackFacade.submit(user, request);
    }

    @GetMapping("/mine")
    public List<FeedbackResponse> getMine(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return feedbackService.getMine(user);
    }

    @GetMapping("/{feedbackId}")
    public FeedbackDetailResponse getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackId
    ) {
        return feedbackFacade.getDetail(user, feedbackId);
    }

    @PatchMapping("/{feedbackId}/accept")
    public void accept(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.accept(user, feedbackId);
    }

    @PatchMapping("/{feedbackId}/reject")
    public void reject(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackRejectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.reject(user, request, feedbackId);
    }

//    @GetMapping("/{feedbackId}/result")
//    public FeedbackResultResponse getResult(@PathVariable UUID feedbackId) {
//        return feedbackService.getResult(feedbackId);
//    }

    @PatchMapping("/{feedbackId}/objection")
    public void object(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackObjectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.object(user, request, feedbackId);
    }

}
