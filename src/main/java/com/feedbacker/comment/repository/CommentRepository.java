package com.feedbacker.comment.repository;

import com.feedbacker.comment.domain.Comment;
import com.feedbacker.comment.domain.type.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /** 작성자는 탈퇴로 null 일 수 있으므로 left join fetch */
    @Query("select c from Comment c left join fetch c.writer where c.id = :commentId")
    Optional<Comment> findWithWriterById(@Param("commentId") UUID commentId);

    /** 게시글의 루트 댓글 목록 (최신순) */
    @Query(value = """
            select c
            from Comment c
            left join fetch c.writer
            where c.targetType = :targetType
              and c.targetId = :targetId
              and c.rootId is null
            order by c.createdAt desc, c.id desc
            """,
            countQuery = """
            select count(c)
            from Comment c
            where c.targetType = :targetType
              and c.targetId = :targetId
              and c.rootId is null
            """)
    Page<Comment> findRoots(
            @Param("targetType") CommentTargetType targetType,
            @Param("targetId") UUID targetId,
            Pageable pageable
    );

    /** 루트 댓글별 답글 수. 결과: [rootId(UUID), count(Long)] */
    @Query("""
            select c.rootId, count(c)
            from Comment c
            where c.rootId in :rootIds
            group by c.rootId
            """)
    List<Object[]> countRepliesByRootIds(@Param("rootIds") Collection<UUID> rootIds);

    long countByRootId(UUID rootId);

    /** 답글 목록 첫 페이지 (오래된 순) */
    @Query("""
            select c
            from Comment c
            left join fetch c.writer
            where c.rootId = :rootId
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findReplies(@Param("rootId") UUID rootId, Pageable pageable);

    /** 답글 목록 다음 페이지: 커서 (createdAt, id) 이후 */
    @Query("""
            select c
            from Comment c
            left join fetch c.writer
            where c.rootId = :rootId
              and (c.createdAt > :cursorCreatedAt
                   or (c.createdAt = :cursorCreatedAt and c.id > :cursorId))
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findRepliesAfter(
            @Param("rootId") UUID rootId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /**
     * 좋아요 수 원자적 증감. like_count 는 엔티티에서 updatable = false 라 네이티브 쿼리로 갱신한다.
     * 감소 시 0 미만으로 내려가지 않게 막는다.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update comments
            set like_count = greatest(like_count + :delta, 0)
            where comment_id = :commentId
            """, nativeQuery = true)
    int addLikeCount(@Param("commentId") UUID commentId, @Param("delta") int delta);

    @Query("select c.likeCount from Comment c where c.id = :commentId")
    int findLikeCountById(@Param("commentId") UUID commentId);
}
