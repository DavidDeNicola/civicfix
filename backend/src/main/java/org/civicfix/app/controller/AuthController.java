package org.civicfix.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.AuthResponse;
import org.civicfix.app.dto.ForgotPasswordRequest;
import org.civicfix.app.dto.LoginRequest;
import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.dto.ResetPasswordRequest;
import org.civicfix.app.model.Role;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.UserRepository;
import org.civicfix.app.security.JwtService;
import org.civicfix.app.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

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
        user.setRole(Role.CITIZEN); //registrazione pubblica --> sempre cittadino

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Utente non trovato"));

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    /**
     * Avvia il recupero password. Risponde sempre 204, anche quando l'email non
     * è registrata: distinguere i due casi permetterebbe di scoprire quali
     * indirizzi hanno un account.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.richiediReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reimpostaPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
