package com.feedbacker.global.image;

import com.feedbacker.feedbackpost.domain.type.ImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ImageRequest(
        @NotNull(message = "이미지 타입은 필수입니다.")
        ImageType type,

        @NotNull(message = "이미지 순서는 필수입니다.")
        @PositiveOrZero(message = "이미지 순서는 0 이상이어야 합니다.")
        Integer order,

        @NotBlank(message = "이미지 경로는 공백일 수 없습니다.")
        @Pattern(regexp = "^images/[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|gif|webp)$",
                message = "올바른 이미지 경로가 아닙니다.")
        String path
) {
    public ImageInfo toImageInfo() {
        return new ImageInfo(type, order, path);
    }
}
