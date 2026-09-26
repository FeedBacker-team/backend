package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.type.ImageType;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.image.ImageResponse;
import com.feedbacker.global.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final SupabaseStorageService storageService;

    public ImageResponse toThumbnailResponse(List<ImageInfo> images) {
        return Stream.ofNullable(images)
                .flatMap(List::stream)
                .filter(image -> image.type() == ImageType.POST_THUMBNAIL)
                .findFirst()
                .map(image -> new ImageResponse(
                        image.type(),
                        image.order(),
                        storageService.createPublicUrl(image.path())
                ))
                .orElse(null);
    }

    public List<ImageResponse> toResponses(List<ImageInfo> images) {
        return Stream.ofNullable(images)
                .flatMap(List::stream)
                .map(image -> new ImageResponse(
                        image.type(),
                        image.order(),
                        storageService.createPublicUrl(image.path())
                ))
                .toList();
    }
}
