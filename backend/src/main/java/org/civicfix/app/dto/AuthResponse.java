package org.civicfix.app.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {
}
