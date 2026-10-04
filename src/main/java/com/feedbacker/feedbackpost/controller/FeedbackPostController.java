package com.feedbacker.feedbackpost.controller;

import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.*;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostSort;
import com.feedbacker.feedbackpost.facade.FeedbackPostFacade;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<FeedbackPostListResponse> getFeedbackPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<ProjectTag> tags,
            @RequestParam(defaultValue = "LATEST") FeedbackPostSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        FeedbackPostListResponse response =
                feedbackPostService.getFeedbackPosts(
                        keyword, tags, sort, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public FeedbackPostCreateResponse create(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody FeedbackPostCreateRequest request
    ) {
        return feedbackPostFacade.create(user.getMemberId(), request);
    }

    @GetMapping("/mine")
    public List<FeedbackSimpleResponse> getMine(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return feedbackPostService.getMine(user.getMemberId());
    }

    @GetMapping("/{feedbackPostId}")
    public FeedbackPostDetailResponse getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        UUID memberId = user == null ? null : user.getMemberId();
        return feedbackPostFacade.getDetail(memberId, feedbackPostId);
    }

    @PostMapping("/{feedbackPostId}/participations")
    public void participate(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        feedbackPostFacade.participate(user.getMemberId(), feedbackPostId);
    }

    @PatchMapping("/{feedbackPostId}/giveup")
    public void giveUp(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        feedbackPostFacade.giveUp(user.getMemberId(), feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/form")
    public FeedbackFormResponse getForm(
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostService.getForm(feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/feedbacks")
    public FeedbackPostProgressResponse getFeedbacks(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostFacade.getFeedbacks(user.getMemberId(), feedbackPostId);
    }

    @PatchMapping("/{feedbackPostId}/complete")
    public void complete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        feedbackPostFacade.complete(user.getMemberId(), feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/result")
    public FeedbackPostResultResponse getResult(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID feedbackPostId
    ) {
        return feedbackPostFacade.getResult(user.getMemberId(), feedbackPostId);
    }

}
