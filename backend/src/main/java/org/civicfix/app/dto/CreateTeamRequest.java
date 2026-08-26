package org.civicfix.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.civicfix.app.model.ReportCategory;

public record CreateTeamRequest(
        @NotBlank String name,
        @NotNull ReportCategory category
) {
}
