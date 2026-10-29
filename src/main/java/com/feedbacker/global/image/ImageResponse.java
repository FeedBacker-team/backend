package com.feedbacker.global.image;

public record ImageResponse(
        ImageType type,
        Integer order,
        String url
) {
}
