package com.feedbacker.comment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 좋아요 기록. (comment_id, member_id) 유니크 제약으로 중복 좋아요를 DB 레벨에서 막는다.
 * 개수는 Comment.likeCount 캐시를 사용하고, 이 테이블은 "누가 눌렀는지" 확인용이다.
 */
@Entity
@Table(name = "comment_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_comment_like_comment_member", columnNames = {"comment_id", "member_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_like_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "comment_id", nullable = false, updatable = false)
    private UUID commentId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public CommentLike(UUID commentId, UUID memberId) {
        this.commentId = commentId;
        this.memberId = memberId;
    }
}
