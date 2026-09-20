package com.feedbacker.project.controller;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.project.domain.dto.request.ProjectCreateRequest;
import com.feedbacker.project.domain.dto.request.ProjectUpdateRequest;
import com.feedbacker.project.domain.dto.response.ProjectCreateResponse;
import com.feedbacker.project.domain.dto.response.ProjectDetailResponse;
import com.feedbacker.project.domain.dto.response.ProjectSummaryResponse;
import com.feedbacker.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/api/projects")
    public ResponseEntity<ProjectCreateResponse> createProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProjectCreateRequest request
    ) {
        ProjectCreateResponse response = projectService.createProject(
                userDetails.getMemberId(),
                request
        );

        URI location = URI.create("/api/projects/" + response.projectId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID viewerId = userDetails == null ? null : userDetails.getMemberId();
        ProjectDetailResponse response = projectService.getProjectDetail(projectId, viewerId);
        return ResponseEntity.ok(response);
    }

    //QA 모집글 만들 때 현재 로그인한 사용자가 선택할 수 있는 프로젝트 목록 조회
    @GetMapping("/api/users/me/projects/available")
    public ResponseEntity<List<ProjectSummaryResponse>> getAvailableProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ProjectSummaryResponse> response = projectService.getAvailableProjects(
                userDetails.getMemberId()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> updateProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        projectService.updateProject(projectId, userDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        projectService.deleteProject(projectId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
