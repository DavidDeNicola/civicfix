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
        long voteCount,
        boolean votedByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** Variante senza dati di voto, per le risposte in cui non sono rilevanti. */
    public static ReportResponse from(Report report) {
        return from(report, 0, false);
    }

    public static ReportResponse from(Report report, long voteCount, boolean votedByCurrentUser) {
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
                voteCount,
                votedByCurrentUser,
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}