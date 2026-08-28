package org.civicfix.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.doc.ForbiddenApiResponse;
import org.civicfix.app.doc.SwaggerTags;
import org.civicfix.app.doc.UnauthorizedApiResponse;
import org.civicfix.app.dto.StatisticsResponse;
import org.civicfix.app.service.StatisticsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
@UnauthorizedApiResponse
@ForbiddenApiResponse
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @Operation(
            summary = "Statistiche delle segnalazioni",
            description = "Conteggi per stato, categoria e priorità, andamento degli ultimi 12 mesi, "
                    + "tempo medio di risoluzione e classifica dei team più attivi. "
                    + "Il tempo medio è null finché non esiste almeno una segnalazione risolta.")
    @ApiResponse(responseCode = "200", description = "Statistiche aggiornate",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StatisticsResponse.class)))
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
