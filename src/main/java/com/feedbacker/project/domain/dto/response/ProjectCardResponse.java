package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectCardResponse(
        @JsonProperty("project_id")
        UUID projectId,

        String title,
        String description,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        List<ProjectTag> tags,

        @JsonProperty("created_at")
        LocalDate createdAt,

        @JsonProperty("view_count")
        long viewCount

        ) {
    public static ProjectCardResponse from(Project project) {
        return new ProjectCardResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getThumbnailImage(),
                project.getTags(),
                project.getCreatedAt().toLocalDate(),
                project.getViewCount()
        );
    }
}
