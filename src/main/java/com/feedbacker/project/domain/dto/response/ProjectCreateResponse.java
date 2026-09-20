package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record ProjectCreateResponse(
        @JsonProperty("project_id")
        UUID projectId
) {
}
