package com.feedbacker.feedbackpost.controller;

import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackFormResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackSimpleResponse;
import com.feedbacker.feedbackpost.facade.FeedbackPostFacade;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback-posts")
@RequiredArgsConstructor
public class FeedbackPostController {

    private final FeedbackPostFacade feedbackPostFacade;
    private final FeedbackPostService feedbackPostService;

    @PostMapping
    public UUID create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackPostCreateRequest request
    ) {
        return feedbackPostFacade.create(user, request);
    }

    @GetMapping("/mine")
    public List<FeedbackSimpleResponse> getMine(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return feedbackPostService.getMine(user);
    }

    @GetMapping("/{feedbackPostId}")
    public FeedbackPostDetailResponse getDetail(
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostService.getDetail(feedbackPostId);
    }

    @PostMapping("/{feedbackPostId}/participations")
    public void participate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        feedbackPostFacade.participate(user, feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/form")
    public FeedbackFormResponse getForm(
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostService.getForm(feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/feedbacks")
    public List<FeedbackProgressResponse> getFeedbacks(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostFacade.getFeedbacks(user, feedbackPostId);
    }

    @PatchMapping("/{feedbackPostId}/complete")
    public void complete(
            @PathVariable UUID feedbackPostId
    ) {
        feedbackPostService.complete(feedbackPostId);
    }

}
