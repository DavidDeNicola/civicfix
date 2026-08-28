package org.civicfix.app.dto;

import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.UpdateType;

import java.time.LocalDateTime;

public record UpdateResponse(
        Long id,
        UpdateType type,
        String message,
        ReportStatus oldStatus,
        ReportStatus newStatus,
        String authorUsername,
        LocalDateTime createdAt
) {
}
