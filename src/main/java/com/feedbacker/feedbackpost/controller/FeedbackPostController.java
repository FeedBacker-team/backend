package com.feedbacker.feedbackpost.controller;

import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.*;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostSort;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback-posts")
@RequiredArgsConstructor
public class FeedbackPostController {

    private final FeedbackPostService feedbackPostService;

    @GetMapping
    public ResponseEntity<FeedbackPostListResponse> getFeedbackPosts(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            List<ProjectTag> tags,

            @RequestParam(defaultValue = "LATEST")
            FeedbackPostSort sort,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "5")
            int size
    ) {
        FeedbackPostListResponse response =
                feedbackPostService.getFeedbackPosts(
                        keyword, tags, sort, page, size);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public void create(@Valid @RequestBody FeedbackPostCreateRequest request) {
        feedbackPostService.create(request);
    }

    @GetMapping("/mine")
    public List<FeedbackSimpleResponse> getMine() {
        return feedbackPostService.getMine();
    }

    @GetMapping("/{feedbackPostId}")
    public FeedbackPostDetailResponse getDetail(@PathVariable UUID feedbackPostId) {
        return feedbackPostService.getDetail(feedbackPostId);
    }

    @PostMapping("/{feedbackPostId}/participations")
    public void participate(@PathVariable UUID feedbackPostId) {
        feedbackPostService.participate(feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/form")
    public FeedbackFormResponse getForm(@PathVariable UUID feedbackPostId) {
        return feedbackPostService.getForm(feedbackPostId);
    }

    @GetMapping("/{feedbackPostId}/feedbacks")
    public List<FeedbackProgressResponse> getFeedbacks(@PathVariable UUID feedbackPostId) {
        return feedbackPostService.getFeedbacks(feedbackPostId);
    }

    @PatchMapping("/{feedbackPostId}/complete")
    public void complete(@PathVariable UUID feedbackPostId) {
        feedbackPostService.complete(feedbackPostId);
    }

}
