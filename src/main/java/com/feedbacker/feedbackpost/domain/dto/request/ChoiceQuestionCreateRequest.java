package com.feedbacker.feedbackpost.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.QuestionType;
import com.feedbacker.global.image.ImageRequest;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.stream.Stream;

public record ChoiceQuestionCreateRequest(

        Integer order,

        @NotBlank(message = "질문 글자는 공백일 수 없습니다.")
        String questionText,

        @NotEmpty(message = "선택지는 최소 1개 이상이어야 합니다.")
        @Size(max = 5, message = "선택지는 최대 5개까지 가능합니다.")
        List<String> optionText,

        @NotNull(message = "객관식 선택 가능 갯수는 공백일 수 없습니다.")
        Integer maxSelectionCount,

        @Size(max = 5, message = "이미지는 최대 5장까지 가능합니다.")
        List<@NotNull ImageRequest> images,

        boolean isRequire
) {

        @AssertTrue(message = "최대 선택 개수는 선택지 개수를 초과할 수 없습니다.")
        @JsonIgnore
        public boolean isMaxSelectionCountValid() {
                if (maxSelectionCount == null || optionText == null) {
                        return true;
                }
                return maxSelectionCount <= optionText.size();
        }

        public Question toEntity() {
                return Question.builder()
                        .questionType(QuestionType.CHOICE)
                        .order(order)
                        .required(isRequire)
                        .questionText(questionText)
                        .optionTexts(optionText)
                        .maxSelectionCount(maxSelectionCount)
                        .images(Stream.ofNullable(images)
                                .flatMap(List::stream)
                                .map(ImageRequest::toImageInfo)
                                .toList())
                        .build();
        }
}
