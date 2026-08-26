package org.civicfix.app.dto;

import jakarta.validation.constraints.NotNull;
import org.civicfix.app.model.ReportStatus;

public record UpdateStatusRequest(
        @NotNull ReportStatus newStatus,
        String message
        ) {
}
