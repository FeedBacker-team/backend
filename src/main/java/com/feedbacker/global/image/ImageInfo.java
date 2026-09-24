package com.feedbacker.global.image;

import com.feedbacker.feedbackpost.domain.type.ImageType;

public record ImageInfo(
        ImageType type,
        Integer order,
        String path
) {
}
