package org.civicfix.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.doc.BadRequestApiResponse;
import org.civicfix.app.doc.ConflictApiResponse;
import org.civicfix.app.doc.SwaggerTags;
import org.civicfix.app.doc.UnauthorizedApiResponse;
import org.civicfix.app.dto.AuthResponse;
import org.civicfix.app.dto.ForgotPasswordRequest;
import org.civicfix.app.dto.LoginRequest;
import org.civicfix.app.dto.RegisterRequest;
import org.civicfix.app.dto.ResetPasswordRequest;
import org.civicfix.app.service.AuthService;
import org.civicfix.app.service.PasswordResetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Endpoint pubblici di autenticazione: registrazione, login, recupero password.
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.PUBBLICO, description = SwaggerTags.PUBBLICO_DESC)
// Annulla il requisito di token dichiarato globalmente in OpenApiConfig: sono
// le rotte con cui il token si ottiene, richiederlo qui sarebbe circolare.
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Registrazione cittadino",
            description = "Crea un nuovo account e restituisce subito il token di accesso. "
                    + "Il ruolo è sempre CITIZEN: non è accettato dal client.")
    @ApiResponse(responseCode = "200", description = "Account creato, token incluso nella risposta",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthResponse.class)))
    @BadRequestApiResponse
    @ConflictApiResponse
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Accesso",
            description = "Verifica le credenziali e restituisce il token JWT da usare nelle chiamate successive.")
    @ApiResponse(responseCode = "200", description = "Accesso effettuato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Richiesta di recupero password",
            description = "Invia il link di reimpostazione all'indirizzo indicato. "
                    + "Risponde 204 anche se l'email non è registrata: distinguere i due casi "
                    + "permetterebbe di scoprire quali indirizzi hanno un account.")
    @ApiResponse(responseCode = "204", description = "Richiesta presa in carico")
    @BadRequestApiResponse
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.richiediReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reimpostazione password",
            description = "Imposta la nuova password consumando il token ricevuto via email. "
                    + "Il token è monouso e scade dopo 30 minuti.")
    @ApiResponse(responseCode = "204", description = "Password reimpostata")
    @BadRequestApiResponse
    @ConflictApiResponse
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reimpostaPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
