package com.feedbacker.feedback.domain.dto.request;

import com.feedbacker.global.image.ImageRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubjectiveQuestionAnswerRequest(

        @NotNull(message = "질문 순서는 필수입니다.")
        @Positive(message = "질문 순서는 1 이상이어야 합니다.")
        Long order,

        String text,

        @Valid
        @Size(max = 1, message = "제출 이미지는 최대 1장까지 가능합니다.")
        List<@NotNull ImageRequest> images
) {
    public SubjectiveQuestionAnswerRequest {
        images = images == null ? List.of() : images;
    }
}
