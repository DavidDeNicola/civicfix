package org.civicfix.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName
) {
}
