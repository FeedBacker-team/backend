package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MyProjectResponse(
        @JsonProperty("project_id")
        UUID projectId,

        String title,

        String description,

        List<ProjectTag> tags,

        @JsonProperty("thumbnail_image")
        String thumbnailImage,

        @JsonProperty("view_count")
        long viewCount,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("has_active_qa")
        boolean hasActiveQa,

        @JsonProperty("active_feedback_post_id")
        UUID activeFeedbackPostId

) {

    public static MyProjectResponse from(
            Project project,
            UUID activeFeedbackPostId
    ) {
        return new MyProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getTags(),
                project.getThumbnailImage(),
                project.getViewCount(),
                project.getCreatedAt(),
                activeFeedbackPostId != null,
                activeFeedbackPostId
        );
    }
}
