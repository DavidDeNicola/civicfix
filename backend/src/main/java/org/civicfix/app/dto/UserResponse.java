package org.civicfix.app.dto;

import org.civicfix.app.model.Role;
import org.civicfix.app.model.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        Long teamId,
        String teamName
) {
    public static UserResponse from (User user){
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole(), user.getTeam() != null ? user.getTeam().getId() : null, user.getTeam() != null ? user.getTeam().getName() : null
        );
    }
}
