package com.feedbacker.comment.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.comment.domain.Comment;
import com.feedbacker.comment.domain.type.CommentStatus;
import com.feedbacker.member.Member;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 응답 (루트 댓글 / 답글 공용).
 * - 루트 댓글: reply_count 포함, root_id 생략
 * - 답글: root_id 포함, reply_count 생략
 * 삭제/숨김 댓글은 작성자와 내용을, 작성자가 탈퇴(물리 삭제)한 댓글은 작성자를 내려주지 않는다.
 */
public record CommentResponse(
        @JsonProperty("comment_id")
        UUID commentId,

        // 답글이 속한 루트 댓글 아이디 (루트 댓글이면 생략)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("root_id")
        UUID rootId,

        @JsonProperty("writer_id")
        UUID writerId,

        @JsonProperty("writer_name")
        String writerName,

        String content,

        CommentStatus status,

        @JsonProperty("like_count")
        int likeCount,

        // 루트 댓글의 답글 수 (답글이면 생략)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("reply_count")
        Long replyCount,

        @JsonProperty("is_liked")
        boolean liked,

        @JsonProperty("is_mine")
        boolean mine,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        // 작성자가 내용을 수정했는지 ("(수정됨)" 표시용)
        @JsonProperty("is_edited")
        boolean edited,

        // 작성자가 내용을 마지막으로 수정한 시각 (수정한 적 없으면 null)
        @JsonProperty("edited_at")
        LocalDateTime editedAt
) {
    /** 루트 댓글 응답 */
    public static CommentResponse ofRoot(
            Comment comment,
            long replyCount,
            boolean liked,
            UUID memberId
    ) {
        return of(comment, replyCount, liked, memberId);
    }

    /** 답글 응답 */
    public static CommentResponse ofReply(
            Comment comment,
            boolean liked,
            UUID memberId
    ) {
        return of(comment, null, liked, memberId);
    }

    private static CommentResponse of(
            Comment comment,
            Long replyCount,
            boolean liked,
            UUID memberId
    ) {
        boolean active = comment.isActive();
        // 삭제/숨김 댓글이거나 작성자가 탈퇴(물리 삭제)한 경우 작성자 정보를 내려주지 않는다.
        Member writer = active ? comment.getWriter() : null;
        return new CommentResponse(
                comment.getId(),
                comment.getRootId(),
                writer != null ? writer.getId() : null,
                writer != null ? writer.getNickname() : null,
                active ? comment.getContent() : null,
                comment.getStatus(),
                comment.getLikeCount(),
                replyCount,
                liked,
                comment.isWrittenBy(memberId),
                comment.getCreatedAt(),
                comment.isEdited(),
                comment.getEditedAt()
        );
    }
}
