package org.civicfix.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.civicfix.app.model.ReportCategory;

public record CreateReportRequest(
        @NotBlank String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull ReportCategory category,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String address
) {
}
