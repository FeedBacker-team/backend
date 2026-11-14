package com.feedbacker.feedback.controller;

import com.feedbacker.feedback.domain.dto.response.AcornHistoryResponse;
import com.feedbacker.feedback.service.AcornHistoryService;
import com.feedbacker.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/acorn-history")
@RequiredArgsConstructor
public class AcornHistoryController {

    private final AcornHistoryService acornHistoryService;

    @GetMapping
    public List<AcornHistoryResponse> getAcornHistory(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return acornHistoryService.getAcornHistory(user);
    }
}
