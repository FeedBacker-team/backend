package com.feedbacker.project.domain.dto.response;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.feedbacker.project.domain.Project;

import java.util.UUID;

public record ProjectSummaryResponse(

        @JsonProperty("project_id")
        UUID projectId,

        String title,

        String description

) {

    public static ProjectSummaryResponse from(Project project) {
        return new ProjectSummaryResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription()
        );
    }
}
