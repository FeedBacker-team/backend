package com.feedbacker.feedback.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.global.image.ImageRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChoiceQuestionAnswerRequest(

        @NotNull(message = "질문 순서는 필수입니다.")
        Long order,

        List<@NotNull @Min(1) @Max(5)Integer> selectedOption,

        @Size(max = 1, message = "제출 이미지는 최대 1장까지 가능합니다.")
        List<@NotNull ImageRequest> images
) {

    @AssertTrue(message = "선택한 번호는 선택한 옵션 개수를 초과할 수 없습니다.")
    @JsonIgnore
    public boolean isSelectedOptionWithinSize() {
        if (selectedOption == null) {
            return true;
        }

        return selectedOption.stream()
                .allMatch(option -> option == null || option <= selectedOption.size());
    }
}
