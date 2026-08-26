package org.civicfix.app.repository;

import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCategory(ReportCategory category);
}
