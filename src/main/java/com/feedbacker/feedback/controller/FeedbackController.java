package com.feedbacker.feedback.controller;

import com.feedbacker.feedback.domain.dto.response.FeedbackDetailResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResponse;
import com.feedbacker.feedback.domain.dto.response.FeedbackResultResponse;
import com.feedbacker.feedback.domain.dto.request.FeedbackSubmitRequest;
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

    private final FeedbackService feedbackService;

    @PostMapping
    public UUID submit(@Valid @RequestBody FeedbackSubmitRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping("/mine")
    public List<FeedbackResponse> getMine() {
        return feedbackService.getMine();
    }

    @GetMapping("/{feedbackId}")
    public FeedbackDetailResponse getDetail(@PathVariable UUID feedbackId) {
        return feedbackService.getDetail(feedbackId);
    }

    @PatchMapping("/{feedbackId}/accept")
    public void accept(@PathVariable UUID feedbackId) {
        feedbackService.accept(feedbackId);
    }

    @PatchMapping("/{feedbackId}/reject")
    public void reject(@PathVariable UUID feedbackId) {
        feedbackService.reject(feedbackId);
    }

//    @GetMapping("/{feedbackId}/result")
//    public FeedbackResultResponse getResult(@PathVariable UUID feedbackId) {
//        return feedbackService.getResult(feedbackId);
//    }

    @PostMapping("/{feedbackId}/objection")
    public void object(@PathVariable UUID feedbackId, @RequestBody String objectReason) {
        feedbackService.object(feedbackId, objectReason);
    }

}
