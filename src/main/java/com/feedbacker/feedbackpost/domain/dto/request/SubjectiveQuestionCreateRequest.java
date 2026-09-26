package com.feedbacker.feedbackpost.domain.dto.request;

import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.global.image.ImageRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Stream;

public record SubjectiveQuestionCreateRequest(

        Integer order,

        @NotBlank(message = "질문 글자는 공백일 수 없습니다.")
        String questionText,

        @Size(max = 5, message = "이미지는 최대 5장까지 가능합니다.")
        List<@NotNull ImageRequest> images,

        boolean isRequire,

        Integer minimumLength
) {
        public Question toEntity() {
                return Question.builder()
                        .questionType(QuestionType.SUBJECTIVE)
                        .order(order)
                        .required(isRequire)
                        .questionText(questionText)
                        .minimumLength(minimumLength)
                        .images(Stream.ofNullable(images)
                                .flatMap(List::stream)
                                .map(ImageRequest::toImageInfo)
                                .toList())
                        .build();
        }
}
