package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.AuthResponse;
import org.civicfix.app.dto.LoginRequest;
import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.UserRepository;
import org.civicfix.app.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Registrazione e accesso. La creazione dell'utente resta in UserService:
 * qui si aggiunge solo ciò che riguarda l'autenticazione, cioè la verifica
 * delle credenziali e l'emissione del token.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        return conToken(userService.creaCittadino(request));
    }

    public AuthResponse login(LoginRequest request) {
        // Solleva BadCredentialsException se le credenziali non tornano;
        // la traduce in 401 il GlobalExceptionHandler.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User utente = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Utente non trovato"));

        return conToken(utente);
    }

    private AuthResponse conToken(User utente) {
        return new AuthResponse(jwtService.generateToken(utente), utente.getUsername(), utente.getRole().name());
    }
}
