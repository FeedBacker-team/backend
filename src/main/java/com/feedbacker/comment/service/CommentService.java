package com.feedbacker.comment.service;

import com.feedbacker.comment.domain.dto.request.CommentCreateRequest;
import com.feedbacker.comment.domain.dto.request.CommentUpdateRequest;
import com.feedbacker.comment.domain.dto.response.CommentLikeResponse;
import com.feedbacker.comment.domain.dto.response.CommentListResponse;
import com.feedbacker.comment.domain.dto.response.CommentReplyListResponse;
import com.feedbacker.comment.domain.dto.response.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

// TODO: 서비스 로직 구현 (현재는 컨트롤러 연결용 시그니처만 있음)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    /** targetId 로 CommentTargetType 판별 후 루트 댓글 목록 조회. memberId 가 null 이면 is_liked / is_mine 은 false */
    public CommentListResponse getComments(UUID memberId, UUID targetId, int page, int size) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** 루트 댓글의 답글 목록 조회. 커서가 null 이면 첫 페이지 */
    public CommentReplyListResponse getReplies(
            UUID memberId, UUID rootCommentId, LocalDateTime cursorCreatedAt, UUID cursorId, int size
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** targetId 로 CommentTargetType 판별 후 루트 댓글 또는 답글 생성 */
    @Transactional
    public CommentResponse create(UUID memberId, UUID targetId, CommentCreateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CommentResponse update(UUID memberId, UUID commentId, CommentUpdateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public void delete(UUID memberId, UUID commentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CommentLikeResponse like(UUID memberId, UUID commentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Transactional
    public CommentLikeResponse unlike(UUID memberId, UUID commentId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
