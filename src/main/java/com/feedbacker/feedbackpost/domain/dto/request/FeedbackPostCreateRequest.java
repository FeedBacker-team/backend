package com.feedbacker.feedbackpost.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Question;
import com.feedbacker.feedbackpost.domain.type.FeedbackPostStatus;
import com.feedbacker.feedbackpost.domain.type.ImageType;
import com.feedbacker.feedbackpost.domain.type.TargetType;
import com.feedbacker.global.image.ImageInfo;
import com.feedbacker.global.image.ImageRequest;
import com.feedbacker.project.domain.Project;
import com.feedbacker.project.domain.ProjectTag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record FeedbackPostCreateRequest(

        @NotNull(message = "프로젝트 아이디는 필수입니다.")
        UUID projectId,

        @NotBlank(message = "제목을 공백일 수 없습니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "설명글은 공백일 수 없습니다.")
        @Size(max = 2000, message = "제목은 2000자 이하여야 합니다.")
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
        LocalDateTime startAt,

        @NotNull(message = "종료일은 값이 필수입니다.")
        @Future(message = "종료일은 과거일 수 없습니다.")
        LocalDateTime endAt,

        @NotNull(message = "타겟은 값이 필수입니다.")
        TargetType target,

        List<ProjectTag> tags,

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

    // 클라이언트-서버 간 네트워크 지연 및 시계 오차 허용 범위
    private static final Duration START_AT_TOLERANCE = Duration.ofMinutes(5);

    @AssertTrue(message = "질문은 최소 1개 이상이어야 합니다.")
    @JsonIgnore
    public boolean isQuestionsNotEmpty() {
        int choice = choiceQuestions == null ? 0 : choiceQuestions.size();
        int subjective = subjectiveQuestions == null ? 0 : subjectiveQuestions.size();
        return choice + subjective >= 1;
    }

    @AssertTrue(message = "질문 순서는 객관식과 주관식을 통틀어 중복될 수 없습니다.")
    @JsonIgnore
    public boolean isQuestionOrderUnique() {
        List<Integer> orders = Stream.concat(
                Stream.ofNullable(choiceQuestions).flatMap(List::stream).map(ChoiceQuestionCreateRequest::order),
                Stream.ofNullable(subjectiveQuestions).flatMap(List::stream).map(SubjectiveQuestionCreateRequest::order)
        ).filter(java.util.Objects::nonNull).toList();
        return orders.stream().distinct().count() == orders.size();
    }

    @AssertTrue(message = "시작일은 과거일 수 없습니다.")
    @JsonIgnore
    public boolean isStartAtValid() {
        return startAt == null || !startAt.isBefore(LocalDateTime.now().minus(START_AT_TOLERANCE));
    }

    @AssertTrue(message = "종료일은 시작일 다음 날 이후여야 합니다.")
    @JsonIgnore
    public boolean isPeriodValid() {
        return startAt == null || endAt == null
                || endAt.toLocalDate().isAfter(startAt.toLocalDate());
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

    // 허용 오차 내 과거 시각은 서버 기준 현재 시각으로 보정
    private LocalDateTime resolveStartAt() {
        LocalDateTime now = LocalDateTime.now();
        return startAt.isBefore(now) ? now : startAt;
    }

    // 종료일은 시각과 무관하게 '그날 끝까지' 모집하도록 보정
    private LocalDateTime resolveEndAt() {
        return endAt.toLocalDate().atTime(23, 59, 59);
    }

    public FeedbackPost toEntity(Project project) {

        List<Question> questions = Stream.concat(
                Stream.ofNullable(choiceQuestions).flatMap(List::stream).map(ChoiceQuestionCreateRequest::toEntity),
                Stream.ofNullable(subjectiveQuestions).flatMap(List::stream).map(SubjectiveQuestionCreateRequest::toEntity)
        ).toList();

        List<ImageInfo> newImages = Stream.ofNullable(this.images)
                .flatMap(List::stream)
                .map(ImageRequest::toImageInfo)
                .collect(Collectors.toCollection(ArrayList::new));
        newImages.add(
                new ImageInfo(ImageType.POST_THUMBNAIL, 0, project.getThumbnailImage())
        );

        return FeedbackPost.builder()
                .writerId(project.getOwner().getId())
                .writerName(project.getOwner().getNickname())
                .title(this.title)
                .status(FeedbackPostStatus.RECRUITING)
                .targetType(this.target)
                .description(this.description)
                .serviceUrl(this.serviceUrl)
                .images(newImages)
                .questions(questions)
                .slotCapacity(this.slotCapacity)
                .remainSlotCount(this.slotCapacity)
                .depositAcorn(this.depositAcorn)
                .rewardAcorn(this.rewardAcorn)
                .startAt(resolveStartAt())
                .endAt(resolveEndAt())
                .project(project)
                .build();
    }
}
