package com.feedbacker.feedback.repository;

import com.feedbacker.feedback.service.AcornHistory;
import com.feedbacker.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AcornHistoryRepository extends JpaRepository<AcornHistory, Long> {
    AcornHistory findByMemberAndFeedbackId(Member member, UUID feedbackId);
}
