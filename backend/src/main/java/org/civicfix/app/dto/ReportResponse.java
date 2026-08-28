package org.civicfix.app.dto;

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
}
