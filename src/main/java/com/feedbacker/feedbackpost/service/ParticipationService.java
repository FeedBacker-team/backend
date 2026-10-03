package com.feedbacker.feedbackpost.service;

import com.feedbacker.feedbackpost.domain.FeedbackPost;
import com.feedbacker.feedbackpost.domain.Participation;
import com.feedbacker.feedbackpost.domain.type.ParticipationStatus;
import com.feedbacker.feedbackpost.exception.ParticipationErrorCode;
import com.feedbacker.feedbackpost.repository.FeedbackPostRepository;
import com.feedbacker.feedbackpost.repository.ParticipationRepository;
import com.feedbacker.global.exception.BusinessException;
import com.feedbacker.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final FeedbackPostRepository feedbackPostRepository;

    private List<Participation> getAllParticipation(UUID feedbackPostId) {
        return participationRepository.findForUpdate(feedbackPostId);
    }

    public Participation getParticipation(UUID feedbackPostId, UUID testerId) {
        return participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId)
                .orElseThrow(() -> new BusinessException(ParticipationErrorCode.PARTICIPATION_NOT_FOUND));
    }

    public Optional<Participation> findParticipation(UUID feedbackPostId, UUID testerId) {
        return participationRepository.findByFeedbackPost_IdAndTester_Id(feedbackPostId, testerId);
    }

    @Transactional
    public int expireOverdueParticipations() {
        LocalDateTime now = LocalDateTime.now();
        List<UUID> feedbackPostIds = participationRepository.findFeedbackPostIdsWithExpiredTargets(
                ParticipationStatus.RESERVED,
                now
        );

        int expiredCount = 0;
        for (UUID feedbackPostId : feedbackPostIds) {
            // participate / giveUp 과 같은 순서(모집글 -> 참여)로 락을 잡아 데드락을 피한다
            FeedbackPost feedbackPost = feedbackPostRepository.findByIdForUpdate(feedbackPostId)
                    .orElse(null);
            if (feedbackPost == null) {
                continue;
            }
            // 락을 잡은 뒤 조회해서, 그 사이 제출/포기된 참여는 만료시키지 않는다
            for (Participation participation : getAllParticipation(feedbackPostId)) {
                if (participation.isReserved() && participation.isDeadlineReached(now)) {
                    participation.expire(now);
                    feedbackPost.plusRemainSlotCount();
                    expiredCount++;
                }
            }
        }
        return expiredCount;
    }

}
