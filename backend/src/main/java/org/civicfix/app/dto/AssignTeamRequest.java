package org.civicfix.app.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTeamRequest(
        @NotNull Long teamId
) {
}
