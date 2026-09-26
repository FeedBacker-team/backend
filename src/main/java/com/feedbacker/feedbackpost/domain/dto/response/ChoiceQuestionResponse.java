package com.feedbacker.feedbackpost.domain.dto.response;

import com.feedbacker.global.image.ImageResponse;
import java.util.List;

public record ChoiceQuestionResponse(
        Integer order,
        String questionText,
        List<String> optionText,
        boolean isSingleSelect,
        List<ImageResponse> images,
        boolean isRequire
) {
}
