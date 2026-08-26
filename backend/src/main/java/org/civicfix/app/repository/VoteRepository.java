package org.civicfix.app.repository;

import org.civicfix.app.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByReportIdAndUserId(Long reportId, Long userId);
    long countByReportId(Long reportId);
    long deleteByReportIdAndUserId(Long reportId, Long userId);
}
