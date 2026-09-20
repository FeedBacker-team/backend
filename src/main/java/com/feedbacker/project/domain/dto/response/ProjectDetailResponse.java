package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProjectDetailResponse(
        @JsonProperty("project_id")
        UUID projectId,

        String title,

        String description,

        List<ProjectTag> tags,

        @JsonProperty("service_link")
        String serviceLink,

        @JsonProperty("thumbnail_image")
        String thumbnailImage,

        @JsonProperty("owner_id")
        UUID ownerId,

        @JsonProperty("owner_nickname")
        String ownerNickname,

        @JsonProperty("view_count")
        long viewCount,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        @JsonProperty("is_owner")
        boolean isOwner,

        @JsonProperty("has_active_qa")
        boolean hasActiveQa,

        @JsonProperty("active_qa")
        ActiveQaResponse activeQa
) {
    public static ProjectDetailResponse from(
            Project project,
            UUID viewerId,
            ActiveQaResponse activeQa
    ) {
        UUID ownerId = project.getOwner().getId();

        boolean isOwner =
                viewerId != null && ownerId.equals(viewerId);

        return new ProjectDetailResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getTags(),
                project.getServiceLink(),
                project.getThumbnailImage(),
                ownerId,
                project.getOwner().getNickname(),
                project.getViewCount(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                isOwner,
                activeQa != null,
                activeQa
        );
    }
}
