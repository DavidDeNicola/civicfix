package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.mapper.UserMapper;
import org.civicfix.app.model.Team;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.TeamRepository;
import org.civicfix.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Creazione e gestione utenti lato admin: valida unicità di username/email,
 * hasha la password e collega opzionalmente un team.
 */

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse createUser(CreateUserRequest request){
        if(userRepository.existsByUsername(request.username())){
            throw new IllegalArgumentException("Username già in uso");
        }
        if(userRepository.existsByEmail(request.email())){
            throw new IllegalArgumentException("Email già in uso");
        }

        Team team = null;
        if(request.teamId() != null){
            team = teamRepository.findById(request.teamId()).orElseThrow(() -> new IllegalArgumentException("Team non trovato"));
        }

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()), team);

        return userMapper.toResponse(userRepository.save(user));
    }

    public List<UserResponse> getAllUsers(){
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    public UserResponse assignTeam(Long userId, Long teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team non trovato"));

        user.setTeam(team);
        return userMapper.toResponse(userRepository.save(user));
    }
}
