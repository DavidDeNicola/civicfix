package org.civicfix.app.repository;

import org.civicfix.app.model.ReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportPhotoRepository extends JpaRepository<ReportPhoto, Long> {
    List<ReportPhoto> findByReportId(Long reportId);
}
