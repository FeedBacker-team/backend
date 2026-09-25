package com.feedbacker.feedback.controller;

import com.feedbacker.feedback.domain.dto.request.FeedbackObjectRequest;
import com.feedbacker.feedback.domain.dto.request.FeedbackRejectRequest;
import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
import com.feedbacker.feedback.facade.FeedbackFacade;
import com.feedbacker.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public UUID submit(@Valid @RequestBody FeedbackSubmitRequest request) {
        return feedbackFacade.submit(request);
    }

    @GetMapping("/mine")
    public List<FeedbackResponse> getMine() {
        return feedbackService.getMine();
    }

    @GetMapping("/{feedbackId}")
    public FeedbackDetailResponse getDetail(@PathVariable UUID feedbackId) {
        return feedbackFacade.getDetail(feedbackId);
    }

    @PatchMapping("/{feedbackId}/accept")
    public void accept(@PathVariable UUID feedbackId) {
        feedbackFacade.accept(feedbackId);
    }

    @PatchMapping("/{feedbackId}/reject")
    public void reject(
            @Valid @RequestBody FeedbackRejectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.reject(request,feedbackId);
    }

//    @GetMapping("/{feedbackId}/result")
//    public FeedbackResultResponse getResult(@PathVariable UUID feedbackId) {
//        return feedbackService.getResult(feedbackId);
//    }

    @PostMapping("/{feedbackId}/objection")
    public void object(
            @Valid @RequestBody FeedbackObjectRequest request,
            @PathVariable UUID feedbackId
    ) {
        feedbackFacade.object(request, feedbackId);
    }

}
