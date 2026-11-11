package com.feedbacker.comment.domain;

import com.feedbacker.comment.domain.type.CommentStatus;
import com.feedbacker.comment.domain.type.CommentTargetType;
import com.feedbacker.comment.exception.CommentErrorCode;
import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 2단계 평탄화 구조: 답글에 다시 답글을 달아도 루트 댓글 아래에 붙고,
 * 누구에게 답했는지는 parentId 로 표현한다.
 * 같은 루트 안의 답글은 (createdAt, id) 순으로 정렬한다.
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_target_root", columnList = "target_type, target_id, root_id, created_at"),
        @Index(name = "idx_comment_root_created", columnList = "root_id, created_at, comment_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // 댓글이 달린 게시글 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20, updatable = false)
    private CommentTargetType targetType;

    // 댓글이 달린 게시글 아이디
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    // 작성자. 회원이 물리 삭제되면 DB 의 ON DELETE SET NULL 로 null 이 된다.
    // 멤버 삭제시 null로 변경되므로, 조회 시 left join fetch 를 사용해야 한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", updatable = false)
    private Member writer;

    // 바로 위 댓글 (루트 댓글이면 null)
    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    // 속한 스레드의 루트 댓글 (루트 댓글이면 null)
    @Column(name = "root_id", updatable = false)
    private UUID rootId;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    // 좋아요 수 캐시. CommentRepository 의 원자적 UPDATE 로만 증감한다.
    // updatable = false 로 dirty checking 이 원자적 UPDATE 결과를 덮어쓰지 못하게 막는다.
    @Column(name = "like_count", nullable = false, updatable = false)
    private int likeCount = 0;

    // 작성자가 내용을 마지막으로 수정한 시각 (수정한 적 없으면 null).
    // updatedAt 은 삭제/숨김/복구에도 갱신되므로 "수정됨" 표시는 이 필드로 판단한다.
    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    private Comment(
            CommentTargetType targetType,
            UUID targetId,
            Member writer,
            UUID parentId,
            UUID rootId,
            String content
    ) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.writer = writer;
        this.parentId = parentId;
        this.rootId = rootId;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
    }

    /** 게시글에 바로 다는 루트 댓글 생성 */
    public static Comment createRoot(
            CommentTargetType targetType,
            UUID targetId,
            Member writer,
            String content
    ) {
        return new Comment(targetType, targetId, writer, null, null, content);
    }

    /** 댓글(루트 또는 답글)에 다는 답글 생성. 항상 루트 댓글 스레드에 평탄화된다. */
    public static Comment createReply(
            Comment parent,
            Member writer,
            String content
    ) {
        parent.validateActive();
        UUID rootId = parent.isRoot() ? parent.getId() : parent.getRootId();
        return new Comment(parent.getTargetType(), parent.getTargetId(), writer,
                parent.getId(), rootId, content);
    }

    /** 작성자가 탈퇴(물리 삭제)되어 알 수 없는 댓글인지 */
    public boolean hasNoWriter() {
        return this.writer == null;
    }

    /** 해당 회원이 작성한 댓글인지 (작성자가 없으면 항상 false) */
    public boolean isWrittenBy(UUID memberId) {
        // LAZY 프록시의 getId() 는 추가 쿼리 없이 식별자만 반환한다.
        return this.writer != null && this.writer.getId().equals(memberId);
    }

    public boolean isRoot() {
        return this.parentId == null;
    }

    public boolean isActive() {
        return this.status == CommentStatus.ACTIVE;
    }

    /** 이 댓글이 해당 게시글에 달린 것인지 */
    public boolean belongsTo(CommentTargetType targetType, UUID targetId) {
        return this.targetType == targetType && this.targetId.equals(targetId);
    }

    /** 내용 수정 (작성자 본인, 정상 상태만) */
    public void updateContent(UUID memberId, String content) {
        validateIsWriter(memberId);
        validateActive();
        this.content = content;
        this.editedAt = LocalDateTime.now();
    }

    public boolean isEdited() {
        return this.editedAt != null;
    }

    /** 작성자 삭제 (소프트). 답글이 없는 경우의 물리 삭제 여부는 서비스에서 판단한다. */
    public void delete(UUID memberId) {
        validateIsWriter(memberId);
        validateActive();
        this.status = CommentStatus.DELETED;
    }

    /** 관리자 조치로 숨김 */
    public void hide() {
        if (this.status == CommentStatus.HIDDEN) {
            throw new BusinessException(CommentErrorCode.COMMENT_ALREADY_HIDDEN);
        }
        this.status = CommentStatus.HIDDEN;
    }

    /** 관리자 숨김 조치 취소 */
    public void restore() {
        if (this.status != CommentStatus.HIDDEN) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_HIDDEN);
        }
        this.status = CommentStatus.ACTIVE;
    }

    /** 작성자 본인인지 검사 (본인이면 통과) */
    public void validateIsWriter(UUID memberId) {
        if (!isWrittenBy(memberId)) {
            throw new BusinessException(CommentErrorCode.COMMENT_ACCESS_DENIED);
        }
    }

    /** 정상 상태인지 검사 (삭제/숨김이면 예외) */
    public void validateActive() {
        if (!isActive()) {
            throw new BusinessException(CommentErrorCode.COMMENT_NOT_ACTIVE);
        }
    }
}
