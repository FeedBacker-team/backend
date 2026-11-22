package com.feedbacker.project.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProjectViewResponse(
        @JsonProperty("view_count") long viewCount
) {
}
