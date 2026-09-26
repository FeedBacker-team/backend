package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackFormResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackPostDetailResponse;
import com.feedbacker.feedbackpost.domain.dto.response.FeedbackSimpleResponse;
import com.feedbacker.feedbackpost.exception.FeedbackPostErrorCode;
import com.feedbacker.feedbackpost.service.mapper.FeedbackFormMapper;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackPostService {

    private final FeedbackPostRepository feedbackPostRepository;
    private final ImageService imageService;
    private final FeedbackFormMapper feedbackFormMapper;

    @Transactional(readOnly = true)
    public FeedbackPostDetailResponse getDetail(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        return FeedbackPostDetailResponse.from(
                feedbackPost,
                imageService.toResponses(feedbackPost.getImages())
        );
    }

    @Transactional(readOnly = true)
    public List<FeedbackSimpleResponse> getMine(CustomUserDetails user) {
        return feedbackPostRepository.findAllByWriterId(user.getMemberId()).stream()
                .map(post -> FeedbackSimpleResponse.from(
                        post,
                        imageService.toThumbnailResponse(post.getImages())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackFormResponse getForm(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        return feedbackFormMapper.toResponse(feedbackPost.getQuestions());
    }

    @Transactional
    public void complete(UUID feedbackPostId) {
        FeedbackPost feedbackPost = getFeedbackPost(feedbackPostId);
        feedbackPost.complete();
    }

    public FeedbackPost getFeedbackPost(UUID feedbackPostId) {
        return feedbackPostRepository.findById(feedbackPostId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND
                ));
    }

    public FeedbackPost getFeedbackPostForUpdate(UUID feedbackPostId) {
        return feedbackPostRepository.findByIdForUpdate(feedbackPostId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackPostErrorCode.FEEDBACK_POST_NOT_FOUND
                ));
    }

    public void validateFeedbackSubmit(FeedbackPost feedbackPost, UUID memberId) {
        feedbackPost.validateIsWriter(memberId);
        feedbackPost.validateRecruiting();
    }

    public void validateParticipation(FeedbackPost feedbackPost, UUID memberId) {
        feedbackPost.validateIsWriter(memberId);
        feedbackPost.validateRecruiting();
    }

    public UUID save(FeedbackPost feedbackPost) {
        FeedbackPost savedFeedback = feedbackPostRepository.save(feedbackPost);
        return savedFeedback.getId();
    }
}
