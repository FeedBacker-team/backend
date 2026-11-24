package com.feedbacker.acorn.repository;

import com.feedbacker.acorn.domain.AcornHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcornHistoryRepository extends JpaRepository<AcornHistory, Long> {
    List<AcornHistory> findAllByMemberIdOrderByTranslateAtDescIdDesc(UUID memberId);
}
