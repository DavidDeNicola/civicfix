package org.civicfix.app.dto;

import org.civicfix.app.model.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        Long teamId,
        String teamName
) {
}
