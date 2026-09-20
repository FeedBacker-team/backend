package com.feedbacker.project.domain.dto.request;

import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjectCreateRequest(
        @NotBlank(message = "프로젝트 제목은 필수입니다.")
        @Size(max = 100, message = "제목은 최대 100자까지 가능합니다.")
        String title,

        @NotBlank(message = "프로젝트 설명은 필수입니다.")
        @Size(max = 2048, message = "설명은 최대 2048자까지 가능합니다.")
        String description,

        @Size(max = 5, message = "태그는 최대 5개까지 선택할 수 있습니다.")
        List<@NotNull(message = "태그는 null일 수 없습니다.") ProjectTag> tags,

        @NotBlank(message = "프로젝트 URL은 필수입니다.")
        @Size(max = 255, message = "프로젝트 URL은 최대 255자까지 가능합니다.")
        @Pattern(
                regexp = "(?i)^https?://[^\\s]+$",
                message = "프로젝트 URL은 HTTP 또는 HTTPS 주소여야 합니다."
        )
        String serviceLink,

        @NotBlank(message = "대표 이미지 URL은 필수입니다.")
        @Size(max = 255, message = "대표 이미지 URL은 최대 255자까지 가능합니다.")
        @Pattern(
                regexp = "(?i)^https?://[^\\s]+$",
                message = "대표 이미지 URL은 HTTP 또는 HTTPS 주소여야 합니다."
        )
        String thumbnailImage
) {
}
