package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.mapper.UserMapper;
import org.civicfix.app.model.Role;
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
        verificaUnicita(request.username(), request.email());

        Team team = null;
        if(request.teamId() != null){
            team = teamRepository.findById(request.teamId()).orElseThrow(() -> new IllegalArgumentException("Team non trovato"));
        }

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()), team);

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Registrazione pubblica. Il ruolo è deciso qui e non accettato dal client:
     * altrimenti chiunque potrebbe registrarsi come amministratore.
     * Restituisce l'entità e non il DTO perché AuthService deve emettere il
     * token, che si firma sull'utente.
     */
    public User creaCittadino(RegisterRequest request){
        verificaUnicita(request.username(), request.email());

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()), Role.CITIZEN);
        return userRepository.save(user);
    }

    /**
     * Il vincolo di unicità sul database resta la garanzia ultima; qui si
     * intercetta prima per rispondere con un messaggio comprensibile.
     */
    private void verificaUnicita(String username, String email){
        if(userRepository.existsByUsername(username)){
            throw new IllegalArgumentException("Username già in uso");
        }
        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("Email già in uso");
        }
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
