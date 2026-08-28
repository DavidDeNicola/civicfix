package org.civicfix.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.doc.BadRequestApiResponse;
import org.civicfix.app.doc.ConflictApiResponse;
import org.civicfix.app.doc.ForbiddenApiResponse;
import org.civicfix.app.doc.SwaggerTags;
import org.civicfix.app.doc.UnauthorizedApiResponse;
import org.civicfix.app.dto.CreateUserRequest;
import org.civicfix.app.dto.UserResponse;
import org.civicfix.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
@UnauthorizedApiResponse
@ForbiddenApiResponse
public class AdminUserController {

    private final UserService userService;

    @Operation(
            summary = "Creazione di un utente",
            description = "Crea un utente con un ruolo qualsiasi. Per gli operatori si può indicare "
                    + "subito il team di appartenenza tramite teamId.")
    @ApiResponse(responseCode = "201", description = "Utente creato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponse.class)))
    @BadRequestApiResponse
    @ConflictApiResponse
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @Operation(summary = "Elenco degli utenti",
            description = "Restituisce tutti gli utenti registrati, con ruolo ed eventuale team.")
    @ApiResponse(responseCode = "200", description = "Elenco degli utenti",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UserResponse.class))))
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Assegnazione del team a un operatore",
            description = "Sposta l'utente nella squadra operativa indicata.")
    @ApiResponse(responseCode = "200", description = "Team assegnato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponse.class)))
    @ConflictApiResponse
    @PutMapping("/{userId}/team/{teamId}")
    public ResponseEntity<UserResponse> assignTeam(@PathVariable Long userId, @PathVariable Long teamId) {
        return ResponseEntity.ok(userService.assignTeam(userId, teamId));
    }
}
