package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.domain.AcornHistory;
import com.feedbacker.feedback.domain.dto.response.AcornHistoryResponse;
import com.feedbacker.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcornHistoryRepository extends JpaRepository<AcornHistory, Long> {
    List<AcornHistory> findAllByMemberIdOrderByTranslateAtDescIdDesc(UUID memberId);

    List<AcornHistory> findAllByMemberIdAndFeedbackPostId(UUID memberId, UUID feedbackPostId);

    AcornHistoryResponse findByMemberIdAndFeedbackPostId(UUID memberId, UUID feedbackPostId);
}
