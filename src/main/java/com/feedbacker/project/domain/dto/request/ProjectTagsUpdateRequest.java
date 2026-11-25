package com.feedbacker.project.domain.dto.request;

import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProjectTagsUpdateRequest(
        @NotNull(message = "태그 목록은 필수입니다.")
        List<@NotNull(message = "태그 목록은 필수입니다.") ProjectTag> tags
        ) {
}
