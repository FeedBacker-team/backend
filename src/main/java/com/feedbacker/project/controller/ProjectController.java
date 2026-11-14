package com.feedbacker.project.controller;

import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.project.domain.ProjectSort;
import com.feedbacker.project.domain.ProjectTag;
import com.feedbacker.project.domain.dto.request.ProjectCreateRequest;
import com.feedbacker.project.domain.dto.request.ProjectUpdateRequest;
import com.feedbacker.project.domain.dto.response.*;
import com.feedbacker.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/api/projects")
    public ResponseEntity<ProjectListResponse> getProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<ProjectTag> tags,
            @RequestParam(defaultValue = "LATEST") ProjectSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        ProjectListResponse response = projectService.getProjects(
                keyword,
                tags,
                sort,
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

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

    // 상세 조회: 쿠키를 발급하지 않고 조회수도 올리지 않음
    @GetMapping("/api/projects/{projectId}")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID memberId = userDetails == null ? null : userDetails.getMemberId();
        return ResponseEntity.ok(projectService.getProject(projectId, memberId));
    }

    // 방문 기록: 실제 상세 화면에서만 호출
    @PostMapping("/api/projects/{projectId}/views")
    public ResponseEntity<ProjectViewResponse> recordProjectView(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @CookieValue(name = "guest_view_id", required = false) String guestCookie
    ) {
        // 로그인 회원은 비회원 쿠키가 있어도 회원 ID로만 집계
        if (userDetails != null) {
            long count = projectService.recordProjectView(
                    projectId, userDetails.getMemberId(), null
            );
            return ResponseEntity.ok(new ProjectViewResponse(count));
        }

        // 비회원은 기존 쿠키를 사용하고, 쿠키가 없거나 잘못됐을 때만 새로 발급
        UUID guestId = parseGuestId(guestCookie);
        boolean needsCookie = guestId == null;
        if (needsCookie) {
            guestId = UUID.randomUUID();
        }

        long count = projectService.recordProjectView(projectId, null, guestId);

        var response = ResponseEntity.ok();
        if (needsCookie) {
            response.header(HttpHeaders.SET_COOKIE, createGuestViewCookie(guestId).toString());
        }
        return response.body(new ProjectViewResponse(count));
    }


    private ResponseCookie createGuestViewCookie(UUID guestId) {
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(seoul);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(seoul);

        long maxAgeSeconds = Duration.between(now, nextMidnight)
                .plusNanos(999_999_999)
                .toSeconds();
        return ResponseCookie.from("guest_view_id", guestId.toString())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/projects")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private UUID parseGuestId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(value);
            return id.toString().equalsIgnoreCase(value) ? id : null;
        } catch (IllegalArgumentException e) {
            return null;
        }

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

    //마이페이지 -> 내 프로젝트 조회
    @GetMapping("/api/users/me/projects")
    public ResponseEntity<List<MyProjectResponse>> getMyProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<MyProjectResponse> response =
                projectService.getMyProjects(
                        userDetails.getMemberId()
                );

        return ResponseEntity.ok(response);
    }



    //프로젝트 수정
    @PutMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> updateProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProjectUpdateRequest request
    ) {
        projectService.updateProject(projectId, userDetails.getMemberId(), request);
        return ResponseEntity.noContent().build();
    }

    //프로젝트 삭제
    @DeleteMapping("/api/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        projectService.deleteProject(projectId, userDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }


}
