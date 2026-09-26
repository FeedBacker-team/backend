package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProjectListResponse(
        @JsonProperty("total_count")
        long totalCount,

        int page,
        int size,

        @JsonProperty("has_next")
        boolean hasNext,

        List<ProjectCardResponse> projects
        ) {
    public static ProjectListResponse from(Page<ProjectCardResponse> projectPage) {
        return new ProjectListResponse(
                projectPage.getTotalElements(),
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.hasNext(),
                projectPage.getContent()
        );
    }
}
