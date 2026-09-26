package com.feedbacker.feedbackpost.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageRequest;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public record FeedbackPostCreateRequest(

        @NotNull(message = "프로젝트 아이디는 필수입니다.")
        UUID projectID,

        @NotBlank(message = "제목을 공백일 수 없습니다.")
        @Size(max = 50, message = "제목은 50자 이하여야 합니다.")
        String title,

        @NotBlank(message = "설명글은 공백일 수 없습니다.")
        @Size(max = 1000, message = "제목은 1000자 이하여야 합니다.")
        String description,

        @NotNull(message = "슬롯 수는 값이 필수입니다.")
        @Positive(message = "슬롯수는 양수여야 합니다.")
        Integer slotCapacity,

        @NotNull(message = "도토리 예치 수는 값이 필수입니다.")
        @Positive(message = "도토리 예치 수는 양수여야 합니다.")
        Integer depositAcorn,

        @NotNull(message = "도토리 보상 수는 값이 필수입니다.")
        @Positive(message = "도토리 보상 수는 양수여야 합니다.")
        Integer rewardAcorn,

        @NotNull(message = "시작일은 값이 필수입니다.")
        @FutureOrPresent(message = "시작일은 과거일 수 없습니다.")
        LocalDateTime startAt,

        @NotNull(message = "시작일은 값이 필수입니다.")
        @Future(message = "시작일은 과거일 수 없습니다.")
        LocalDateTime endAt,

        @NotNull(message = "타겟은 값이 필수입니다.")
        TargetType target,

        List<ProjectTag> tags,

        @NotBlank(message = "링크는 공백일 수 없습니다.")
        @Pattern(regexp = "^https?://.+", message = "링크는 https 타입이어야 합니다.")
        String serviceUrl,

        @Valid
        @Size(max = 5, message = "이미지는 최대 5장까지 가능합니다.")
        List<@NotNull ImageRequest> images,

        @Valid
        List<ChoiceQuestionCreateRequest> choiceQuestions,

        @Valid
        List<SubjectiveQuestionCreateRequest> subjectiveQuestions

) {

    @AssertTrue(message = "질문은 최소 1개 이상이어야 합니다.")
    @JsonIgnore
    public boolean isQuestionsNotEmpty() {
        int choice = choiceQuestions == null ? 0 : choiceQuestions.size();
        int subjective = subjectiveQuestions == null ? 0 : subjectiveQuestions.size();
        return choice + subjective >= 1;
    }

    @AssertTrue(message = "종료일은 시작일 이후여야 합니다.")
    @JsonIgnore
    public boolean isPeriodValid() {
        return startAt == null || endAt == null || endAt.isAfter(startAt);
    }

    @AssertTrue(message = "도토리 예치 수를 슬롯 수로 나눈 값은 도토리 보상 수와 같아야 합니다.")
    @JsonIgnore
    public boolean isRewardAcornValid() {
        if (depositAcorn == null || slotCapacity == null || rewardAcorn == null || slotCapacity <= 0) {
            return true;
        }
        return depositAcorn % slotCapacity == 0
                && depositAcorn / slotCapacity == rewardAcorn;
    }

    public FeedbackPost toEntity(Project project) {

        List<Question> questions = Stream.concat(
                Stream.ofNullable(choiceQuestions).flatMap(List::stream).map(ChoiceQuestionCreateRequest::toEntity),
                Stream.ofNullable(subjectiveQuestions).flatMap(List::stream).map(SubjectiveQuestionCreateRequest::toEntity)
        ).toList();

        return FeedbackPost.builder()
                .title(title)
                .status(FeedbackPostStatus.RECRUITING)
                .targetType(target)
                .description(description)
                .serviceUrl(serviceUrl)
                .images(Stream.ofNullable(images)
                        .flatMap(List::stream)
                        .map(ImageRequest::toImageInfo)
                        .toList())
                .questions(questions)
                .slotCapacity(slotCapacity)
                .remainSlotCount(slotCapacity)
                .depositAcorn(depositAcorn)
                .rewardAcorn(rewardAcorn)
                .startAt(startAt)
                .endAt(endAt)
                .project(project)
                .build();
    }
}
