package com.feedbacker.comment.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/** 루트 댓글 목록 (페이지 방식) */
public record CommentListResponse(
        @JsonProperty("total_count")
        long totalCount,

        int page,

        int size,

        @JsonProperty("has_next")
        boolean hasNext,

        List<CommentResponse> comments
) {
    public static CommentListResponse from(Page<CommentResponse> page) {
        return new CommentListResponse(
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext(),
                page.getContent()
        );
    }
}
