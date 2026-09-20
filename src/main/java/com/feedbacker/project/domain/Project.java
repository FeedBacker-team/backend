package com.feedbacker.project.domain;

import com.feedbacker.global.common.BaseTimeEntity;
import com.feedbacker.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "project_id",
            columnDefinition = "uuid",
            updatable = false,
            nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 2048, nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(
            name = "project_tags",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @OrderColumn(name = "tag_order")
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    private List<ProjectTag> tags = new ArrayList<>();

    @Column(name = "service_link", nullable = false)
    private String serviceLink;

    @Column(name = "thumbnail_image", nullable = false)
    private String thumbnailImage;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;


    private Project(
            Member owner,
            String title,
            String description,
            List<ProjectTag> tags,
            String serviceLink,
            String thumbnailImage
    ) {

        validateOwner(owner);
        validateProjectInfo(title, description, tags, serviceLink, thumbnailImage);
        this.owner = owner;
        this.status = ProjectStatus.PUBLISHED;
        this.title = title;
        this.description = description;
        this.serviceLink = serviceLink;
        this.thumbnailImage = thumbnailImage;
        this.viewCount = 0L;

        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public static Project create(
            Member owner,
            String title,
            String description,
            List<ProjectTag> tags,
            String serviceLink,
            String thumbnailImage
    ) {
        return new Project(
                owner,
                title,
                description,
                tags,
                serviceLink,
                thumbnailImage
        );
    }

    public void update(
            String title,
            String description,
            List<ProjectTag> tags,
            String serviceLink,
            String thumbnailImage
    ) {
        validateNotDeleted();
        validateProjectInfo(title, description, tags, serviceLink, thumbnailImage);

        this.title = title;
        this.description = description;
        this.serviceLink = serviceLink;
        this.thumbnailImage = thumbnailImage;

        this.tags.clear();

        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public List<ProjectTag> getTags() {
        return List.copyOf(tags);
    }

    private void validateNotDeleted() {
        if (status == ProjectStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 프로젝트입니다.");
        }

    }
    public void delete() {
        validateNotDeleted();
        this.status = ProjectStatus.DELETED;

    }

    public void increaseViewCount() {
        validateNotDeleted();
        this.viewCount++;
    }

    private void validateOwner(Member owner) {
        if (owner == null) {
            throw new IllegalArgumentException(
                    "프로젝트 작성자는 필수입니다."
            );
        }
    }

    private void validateProjectInfo(
            String title,
            String description,
            List<ProjectTag> tags,
            String serviceLink,
            String thumbnailImage
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "프로젝트 제목은 필수입니다."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "프로젝트 설명은 필수입니다."
            );
        }

        if (serviceLink == null || serviceLink.isBlank()) {
            throw new IllegalArgumentException("서비스 링크는 필수입니다.");
        }

        if (thumbnailImage == null || thumbnailImage.isBlank()) {
            throw new IllegalArgumentException(
                    "프로젝트 대표 이미지는 필수입니다."
            );
        }

        validateTags(tags);
    }


    private void validateTags(List<ProjectTag> tags) {
        if (tags == null) {
            return;
        }
        if (tags.size() > 5) {
            throw new IllegalArgumentException("태그는 최대 5개까지 선택할 수 있습니다");
        }
        if (new HashSet<>(tags).size() != tags.size()) {
            throw new IllegalArgumentException("중복된 태그를 선택할 수 없습니다.");
        }

    }
}
