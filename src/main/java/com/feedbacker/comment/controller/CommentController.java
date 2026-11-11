package com.feedbacker.comment.controller;

import com.feedbacker.comment.domain.dto.request.CommentCreateRequest;
import com.feedbacker.comment.domain.dto.request.CommentUpdateRequest;
import com.feedbacker.comment.domain.dto.response.CommentLikeResponse;
import com.feedbacker.comment.domain.dto.response.CommentListResponse;
import com.feedbacker.comment.domain.dto.response.CommentReplyListResponse;
import com.feedbacker.comment.domain.dto.response.CommentResponse;
import com.feedbacker.comment.service.CommentService;
import com.feedbacker.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 API (피드백 모집글 / 프로젝트 게시글 공용).
 * 조회(GET)는 비로그인 허용, 생성·수정·삭제·좋아요는 로그인 필수 (SecurityConfig 참고).
 * 게시글 유형(CommentTargetType)은 서비스에서 targetId 로 판별한다.
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 게시글의 루트 댓글 목록 조회 (페이지 방식). targetId 는 게시글 아이디 */
    @GetMapping("/{targetId}")
    public CommentListResponse getComments(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID memberId = user == null ? null : user.getMemberId();
        return commentService.getComments(memberId, targetId, page, size);
    }

    /**
     * 루트 댓글의 답글 목록 조회 (커서 방식).
     * 첫 요청은 커서 없이, 다음 요청은 응답의 next_cursor_created_at / next_cursor_id 를 넘긴다.
     */
    @GetMapping("/{commentId}/replies")
    public CommentReplyListResponse getReplies(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID commentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "10") int size
    ) {
        UUID memberId = user == null ? null : user.getMemberId();
        return commentService.getReplies(memberId, commentId, cursorCreatedAt, cursorId, size);
    }

    /** 댓글 생성. 경로의 id 는 게시글 아이디 (parent_id 가 있으면 답글) */
    @PostMapping("/{targetId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID targetId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        return commentService.create(user.getMemberId(), targetId, request);
    }

    /** 댓글 수정 */
    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        return commentService.update(user.getMemberId(), commentId, request);
    }

    /** 댓글 삭제 */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID commentId
    ) {
        commentService.delete(user.getMemberId(), commentId);
    }

    /** 좋아요 */
    @PostMapping("/{commentId}/likes")
    public CommentLikeResponse like(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID commentId
    ) {
        return commentService.like(user.getMemberId(), commentId);
    }

    /** 좋아요 취소 */
    @DeleteMapping("/{commentId}/likes")
    public CommentLikeResponse unlike(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID commentId
    ) {
        return commentService.unlike(user.getMemberId(), commentId);
    }
}
