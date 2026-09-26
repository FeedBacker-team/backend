package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.global.image.ImageResponse;
import java.util.List;

public record SubjectiveQuestionResponse(
        Integer order,
        String questionText,
        List<ImageResponse> images,
        boolean isRequire,
        Integer minimumLength
) {
}
