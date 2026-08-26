package org.civicfix.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.civicfix.app.model.Role;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        @NotNull Role role,
        Long teamId //opzionale, solo se role = OPERATOR
) {
}
