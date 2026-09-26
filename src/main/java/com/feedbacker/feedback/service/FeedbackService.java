package com.feedbacker.feedback.service;

import com.feedbacker.feedback.domain.Feedback;
import com.feedbacker.feedback.domain.QuestionAnswer;
import com.feedbacker.feedback.domain.dto.response.*;
import com.feedbacker.feedback.exception.FeedbackErrorCode;
import com.feedbacker.feedback.repository.FeedbackRepository;
import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.global.security.CustomUserDetails;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public UUID submit(
            FeedbackPost feedbackPost,
            Member tester,
            List<QuestionAnswer> answers
    ) {
        Feedback savedFeedback = feedbackRepository.save(
                Feedback.create(feedbackPost, tester, answers)
        );
        return savedFeedback.getId();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getMine(CustomUserDetails user) {
        List<Feedback> feedbacks = feedbackRepository.getAllByTesterId(user.getMemberId());
        return FeedbackResponse.fromAll(feedbacks);
    }

//    @Transactional(readOnly = true)
//    public FeedbackResultResponse getResult(UUID feedbackId) {
//        Feedback feedback = getFeedback(feedbackId);
//        FeedbackPost feedbackPost = getFeedbackPost(feedback.getFeedbackPostId());
//
//        UUID memberId = UUID.randomUUID();
//        Member member = getMember(memberId);
//
//        AcornHistoryResponse acornHistoryResponse = AcornHistoryResponse.from(
//                acornHistoryRepository.findByMemberAndFeedbackId(member, feedbackId)
//        );
//
//        return FeedbackResultResponse.from(feedback, feedbackPost, acornHistoryResponse);
//    }

    public Feedback getFeedback(UUID feedbackId) {
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(
                        FeedbackErrorCode.FEEDBACK_NOT_FOUND
                ));
    }

    public void validateGetFeedback(
            UUID memberId,
            Feedback feedback,
            FeedbackPost feedbackPost
    ) {
        if (!memberId.equals(feedback.getTesterId())
                || !memberId.equals(feedbackPost.getWriterId())) {
            throw new BusinessException(FeedbackErrorCode.FEEDBACK_ACCESS_DENIED);
        }
    }
}
