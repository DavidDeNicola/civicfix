package org.civicfix.app.repository;

import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.Update;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UpdateRepository extends JpaRepository<Update, Long> {
    List<Update> findByReportIdOrderByCreatedAtAsc(Long reportId);

    List<Update> findByNewStatus(ReportStatus newStatus);
}
