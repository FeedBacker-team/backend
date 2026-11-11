package com.feedbacker.comment.repository;

import com.feedbacker.comment.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    boolean existsByCommentIdAndMemberId(UUID commentId, UUID memberId);

    /** 삭제된 행 수를 반환 (0 이면 좋아요를 누르지 않은 상태) */
    @Modifying(flushAutomatically = true)
    @Query("delete from CommentLike l where l.commentId = :commentId and l.memberId = :memberId")
    int deleteByCommentIdAndMemberId(@Param("commentId") UUID commentId, @Param("memberId") UUID memberId);

    /** 주어진 댓글 중 회원이 좋아요를 누른 댓글 아이디 */
    @Query("""
            select l.commentId
            from CommentLike l
            where l.memberId = :memberId
              and l.commentId in :commentIds
            """)
    Set<UUID> findLikedCommentIds(
            @Param("memberId") UUID memberId,
            @Param("commentIds") Collection<UUID> commentIds
    );
}
