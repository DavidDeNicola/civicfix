package org.civicfix.app.dto;

import jakarta.validation.constraints.NotNull;

public record AssignOperatorRequest(
        @NotNull Long operatorId
) {
}
