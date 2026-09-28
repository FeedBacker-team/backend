package com.feedbacker.tag;

import com.feedbacker.project.domain.ProjectTag;

public record TagResponse(
        String code,
        String displayName
) {
    public static TagResponse from(ProjectTag tag) {
        return new TagResponse(
                tag.name(),
                tag.getDisplayName()
        );
    }
}
