package org.civicfix.app.mapper;

import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.model.Role;
import org.civicfix.app.model.Team;
import org.civicfix.app.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getTeam() != null ? user.getTeam().getId() : null,
                user.getTeam() != null ? user.getTeam().getName() : null
        );
    }

    /**
     * Password già cifrata e team già risolto arrivano dal service: il mapper
     * non deve conoscere il PasswordEncoder né interrogare i repository.
     */
    public User toEntity(CreateUserRequest request, String passwordHash, Team team) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordHash);
        user.setFullName(request.fullName());
        user.setRole(request.role());
        user.setTeam(team);
        return user;
    }

    /**
     * Variante per la registrazione pubblica, che ha meno campi: il ruolo non
     * è nella richiesta ma lo decide il service, così la regola "chi si
     * registra è sempre un cittadino" resta visibile lì e non qui.
     */
    public User toEntity(RegisterRequest request, String passwordHash, Role role) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordHash);
        user.setFullName(request.fullName());
        user.setRole(role);
        return user;
    }
}
