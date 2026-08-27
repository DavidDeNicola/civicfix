package org.civicfix.app.dto;

import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.ReportPriority;
import org.civicfix.app.model.ReportStatus;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        String title,
        String description,
        ReportCategory category,
        ReportStatus status,
        ReportPriority priority,
        Double latitude,
        Double longitude,
        String address,
        String reportedUsername,
        String assignedTeamName,
        String assignedOperatorUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReportResponse from(Report report){
        return new ReportResponse(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getCategory(),
                report.getStatus(),
                report.getPriority(),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddress(),
                report.getReporter().getUsername(),
                report.getAssignedTeam() != null ? report.getAssignedTeam().getName() : null,
                report.getAssignedOperator() != null ? report.getAssignedOperator().getUsername() : null,
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}