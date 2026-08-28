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
import org.civicfix.app.doc.ForbiddenApiResponse;
import org.civicfix.app.doc.SwaggerTags;
import org.civicfix.app.doc.UnauthorizedApiResponse;
import org.civicfix.app.dto.CreateTeamRequest;
import org.civicfix.app.dto.TeamResponse;
import org.civicfix.app.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
@UnauthorizedApiResponse
@ForbiddenApiResponse
public class AdminTeamController {

    private final TeamService teamService;

    @Operation(summary = "Creazione di un team",
            description = "Crea una squadra operativa legata a una categoria di segnalazioni.")
    @ApiResponse(responseCode = "201", description = "Team creato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TeamResponse.class)))
    @BadRequestApiResponse
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @Operation(summary = "Elenco dei team",
            description = "Restituisce tutte le squadre operative con il numero di membri.")
    @ApiResponse(responseCode = "200", description = "Elenco dei team",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = TeamResponse.class))))
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }
}
