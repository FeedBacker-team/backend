package com.feedbacker.comment.service;

import com.feedbacker.comment.domain.type.CommentTargetType;
import com.feedbacker.comment.exception.CommentErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.project.domain.ProjectStatus;
import com.feedbacker.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 게시글 아이디로 댓글 대상 유형을 판별한다.
 * 피드백 모집글과 프로젝트는 모두 UUID 라 아이디가 겹치지 않는다는 전제를 둔다.
 */
@Component
@RequiredArgsConstructor
public class CommentTargetResolver {

    private final FeedbackPostRepository feedbackPostRepository;
    private final ProjectRepository projectRepository;

    /** 존재하는 게시글의 유형을 반환. 없거나 삭제된 게시글이면 예외 */
    public CommentTargetType resolve(UUID targetId) {
        if (feedbackPostRepository.existsById(targetId)) {
            return CommentTargetType.FEEDBACK_POST;
        }
        if (projectRepository.existsByIdAndStatus(targetId, ProjectStatus.PUBLISHED)) {
            return CommentTargetType.PROJECT_POST;
        }
        throw new BusinessException(CommentErrorCode.COMMENT_TARGET_NOT_FOUND);
    }
}
