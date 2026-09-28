package com.feedbacker.global.image;

import com.feedbacker.feedbackpost.domain.type.ImageType;

public record ImageResponse(
        ImageType type,
        Integer order,
        String url
) {
}
