package com.feedbacker.feedbackpost.controller;

import com.feedbacker.feedbackpost.domain.dto.request.FeedbackPostCreateRequest;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackFormResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackProgressResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackSimpleResponse;
import com.feedbacker.feedbackpost.service.FeedbackPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback-posts")
@RequiredArgsConstructor
public class FeedbackPostController {

    private final FeedbackPostService feedbackPostService;

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
