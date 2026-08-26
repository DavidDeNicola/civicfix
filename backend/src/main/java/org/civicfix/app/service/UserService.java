package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.model.Team;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.TeamRepository;
import org.civicfix.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createUser(CreateUserRequest request){
        if(userRepository.existsByUsername(request.username())){
            throw new IllegalArgumentException("Username già in uso");
        }
        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("Email già in uso");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());

        if(request.teamId() != null){
            Team team = teamRepository.findById(request.teamId()).orElseThrow(() -> new IllegalArgumentException("Team non trovato"));
            user.setTeam(team);
        }

        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse assignTeam(Long userId, Long teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team non trovato"));

        user.setTeam(team);
        return UserResponse.from(userRepository.save(user));
    }
}
