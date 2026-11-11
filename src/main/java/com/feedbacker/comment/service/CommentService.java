package com.feedbacker.comment.service;

import com.feedbacker.comment.domain.Comment;
import com.feedbacker.comment.domain.CommentLike;
import com.feedbacker.comment.domain.dto.request.CommentCreateRequest;
import com.feedbacker.comment.domain.dto.request.CommentUpdateRequest;
import com.feedbacker.comment.domain.dto.response.CommentLikeResponse;
import com.feedbacker.comment.domain.dto.response.CommentListResponse;
import com.feedbacker.comment.domain.dto.response.CommentReplyListResponse;
import com.feedbacker.comment.domain.dto.response.CommentResponse;
import com.feedbacker.comment.domain.type.CommentTargetType;
import com.feedbacker.comment.exception.CommentErrorCode;
import com.feedbacker.comment.repository.CommentLikeRepository;
import com.feedbacker.comment.repository.CommentRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import com.feedbacker.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentTargetResolver commentTargetResolver;
    private final MemberService memberService;

    /** targetId 로 CommentTargetType 판별 후 루트 댓글 목록 조회. memberId 가 null 이면 is_liked / is_mine 은 false */
    public CommentListResponse getComments(UUID memberId, UUID targetId, int page, int size) {
        CommentTargetType targetType = commentTargetResolver.resolve(targetId);

        Page<Comment> roots = commentRepository.findRoots(
                targetType, targetId, PageRequest.of(Math.max(page, 0), clampSize(size)));

        List<UUID> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        Map<UUID, Long> replyCounts = countReplies(rootIds);
        Set<UUID> likedIds = findLikedIds(memberId, rootIds);

        Page<CommentResponse> responses = roots.map(c -> CommentResponse.ofRoot(
                c,
                replyCounts.getOrDefault(c.getId(), 0L),
                likedIds.contains(c.getId()),
                memberId
        ));
        return CommentListResponse.from(responses);
    }

    /** 루트 댓글의 답글 목록 조회 (오래된 순). 커서가 null 이면 첫 페이지 */
    public CommentReplyListResponse getReplies(
            UUID memberId, UUID rootCommentId, LocalDateTime cursorCreatedAt, UUID cursorId, int size
    ) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new BusinessException(CommentErrorCode.INVALID_REPLY_CURSOR);
        }
        Comment root = getComment(rootCommentId);
        if (!root.isRoot()) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_ROOT);
        }

        int limit = clampSize(size);
        // 다음 페이지 존재 여부를 알기 위해 하나 더 조회
        PageRequest fetch = PageRequest.of(0, limit + 1);
        List<Comment> fetched = cursorId == null
                ? commentRepository.findReplies(rootCommentId, fetch)
                : commentRepository.findRepliesAfter(rootCommentId, cursorCreatedAt, cursorId, fetch);

        boolean hasNext = fetched.size() > limit;
        List<Comment> replies = hasNext ? fetched.subList(0, limit) : fetched;

        Set<UUID> likedIds = findLikedIds(memberId, replies.stream().map(Comment::getId).toList());
        List<CommentResponse> responses = replies.stream()
                .map(c -> CommentResponse.ofReply(c, likedIds.contains(c.getId()), memberId))
                .toList();
        return CommentReplyListResponse.of(responses, hasNext);
    }

    /** targetId 로 CommentTargetType 판별 후 루트 댓글 또는 답글 생성 */
    @Transactional
    public CommentResponse create(UUID memberId, UUID targetId, CommentCreateRequest request) {
        CommentTargetType targetType = commentTargetResolver.resolve(targetId);
        Member writer = memberService.getMember(memberId);

        if (!request.isReply()) {
            Comment comment = commentRepository.save(
                    Comment.createRoot(targetType, targetId, writer, request.content()));
            return CommentResponse.ofRoot(comment, 0L, false, memberId);
        }

        Comment parent = getComment(request.parentId());
        if (!parent.belongsTo(targetType, targetId)) {
            throw new BusinessException(CommentErrorCode.INVALID_PARENT_COMMENT);
        }
        Comment reply = commentRepository.save(Comment.createReply(parent, writer, request.content()));
        return CommentResponse.ofReply(reply, false, memberId);
    }

    @Transactional
    public CommentResponse update(UUID memberId, UUID commentId, CommentUpdateRequest request) {
        Comment comment = getComment(commentId);
        comment.updateContent(memberId, request.content());

        boolean liked = commentLikeRepository.existsByCommentIdAndMemberId(commentId, memberId);
        return comment.isRoot()
                ? CommentResponse.ofRoot(comment, commentRepository.countByRootId(commentId), liked, memberId)
                : CommentResponse.ofReply(comment, liked, memberId);
    }

    /**
     * 답글 유무와 관계없이 항상 소프트 삭제한다.
     * 조회에서 숨기지 않고 "삭제된 댓글입니다"로 보이며, 답글은 그대로 남는다.
     * 신고된 댓글의 내용을 관리자가 확인할 수 있도록 행을 지우지 않는다.
     */
    @Transactional
    public void delete(UUID memberId, UUID commentId) {
        Comment comment = getComment(commentId);
        comment.delete(memberId);
    }

    @Transactional
    public CommentLikeResponse like(UUID memberId, UUID commentId) {
        Comment comment = getComment(commentId);
        comment.validateActive();

        if (commentLikeRepository.existsByCommentIdAndMemberId(commentId, memberId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_ALREADY_LIKED);
        }
        try {
            // 동시 요청은 (comment_id, member_id) 유니크 제약으로 걸러진다
            commentLikeRepository.saveAndFlush(new CommentLike(commentId, memberId));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommentErrorCode.COMMENT_ALREADY_LIKED);
        }
        commentRepository.addLikeCount(commentId, 1);

        return new CommentLikeResponse(commentId, commentRepository.findLikeCountById(commentId), true);
    }

    /** 좋아요 취소. 삭제/숨김된 댓글이어도 취소는 허용한다. */
    @Transactional
    public CommentLikeResponse unlike(UUID memberId, UUID commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND);
        }
        int deleted = commentLikeRepository.deleteByCommentIdAndMemberId(commentId, memberId);
        if (deleted == 0) {
            throw new BusinessException(CommentErrorCode.COMMENT_LIKE_NOT_FOUND);
        }
        commentRepository.addLikeCount(commentId, -1);

        return new CommentLikeResponse(commentId, commentRepository.findLikeCountById(commentId), false);
    }

    private Comment getComment(UUID commentId) {
        return commentRepository.findWithWriterById(commentId)
                .orElseThrow(() -> new BusinessException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private Map<UUID, Long> countReplies(Collection<UUID> rootIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (rootIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : commentRepository.countRepliesByRootIds(rootIds)) {
            counts.put((UUID) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Set<UUID> findLikedIds(UUID memberId, Collection<UUID> commentIds) {
        if (memberId == null || commentIds.isEmpty()) {
            return Set.of();
        }
        return commentLikeRepository.findLikedCommentIds(memberId, commentIds);
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
