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
import org.civicfix.app.dto.*;
import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.security.CustomUserDetails;
import org.civicfix.app.service.ReportService;
import org.civicfix.app.service.VoteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.civicfix.app.dto.AssignPriorityRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final VoteService voteService;

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(
            summary = "Apertura di una segnalazione",
            description = "Crea una segnalazione a nome dell'utente autenticato. "
                    + "Nasce sempre in stato PENDING e con priorità NORMAL.")
    @ApiResponse(responseCode = "201", description = "Segnalazione creata",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(request, currentUser.getId()));
    }

    @Tag(name = SwaggerTags.PUBBLICO, description = SwaggerTags.PUBBLICO_DESC)
    @Operation(
            summary = "Dettaglio di una segnalazione",
            description = "Consultabile senza autenticazione. Con un token valido, il campo "
                    + "votedByCurrentUser indica se chi legge ha già espresso il proprio sostegno.")
    @ApiResponse(responseCode = "200", description = "Segnalazione trovata",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @ConflictApiResponse
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        // La lettura è pubblica: senza sessione il principal è null e la
        // segnalazione risulta semplicemente non votata.
        return ResponseEntity.ok(reportService.getById(id, idOppureNull(currentUser)));
    }

    /**
     * Ricerca paginata. Categorie e stati accettano più valori perché il
     * pannello filtri è a selezione multipla; titolo e intervallo di date
     * sono qui e non nel browser perché altrimenti filtrerebbero soltanto la
     * pagina già scaricata.
     */
    @Tag(name = SwaggerTags.PUBBLICO, description = SwaggerTags.PUBBLICO_DESC)
    @Operation(
            summary = "Ricerca segnalazioni",
            description = "Elenco paginato con filtri combinabili. Categorie e stati accettano "
                    + "più valori; lat, lng e radiusKm vanno forniti insieme per la ricerca per vicinanza.")
    // Nessun @ApiResponse esplicito: dichiararlo senza "content" cancellerebbe lo
    // schema dedotto, e PagedResponse<ReportResponse> come tipo generico sa
    // descriverlo correttamente solo springdoc.
    @GetMapping
    public ResponseEntity<PagedResponse<ReportResponse>> search(
            @RequestParam(required = false) List<ReportCategory> categories,
            @RequestParam(required = false) List<ReportStatus> statuses,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        return ResponseEntity.ok(reportService.searchReports(
                categories, statuses, title, from, to, lat, lng, radiusKm,
                page, size, idOppureNull(currentUser)));
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(
            summary = "Modifica di una propria segnalazione",
            description = "Concessa solo all'autore e solo finché la segnalazione è in stato PENDING: "
                    + "una volta presa in carico, cambiarla sotto chi ci lavora renderebbe incoerente la cronologia.")
    @ApiResponse(responseCode = "200", description = "Segnalazione aggiornata",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PutMapping("/{id}")
    public ResponseEntity<ReportResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(reportService.updateReport(id, request, currentUser.getId()));
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(
            summary = "Eliminazione di una propria segnalazione",
            description = "Come la modifica: solo l'autore e solo in stato PENDING. "
                    + "Rimuove anche sostegni, foto e relativi file su disco.")
    @ApiResponse(responseCode = "204", description = "Segnalazione eliminata")
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        reportService.deleteReport(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    private Long idOppureNull(CustomUserDetails currentUser) {
        return currentUser != null ? currentUser.getId() : null;
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(
            summary = "Sostieni una segnalazione",
            description = "Aggiunge il proprio sostegno. Non è consentito sostenere una segnalazione "
                    + "aperta da sé stessi né una già chiusa; il doppio voto è ignorato.")
    @ApiResponse(responseCode = "200", description = "Conteggio aggiornato dei sostegni",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = VoteResponse.class)))
    @UnauthorizedApiResponse
    @ConflictApiResponse
    @PostMapping("/{id}/vote")
    public ResponseEntity<VoteResponse> vota(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(voteService.vota(id, currentUser.getId()));
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(
            summary = "Ritira il sostegno",
            description = "Rimuove il proprio sostegno. Se non era stato espresso, l'operazione non ha effetto.")
    @ApiResponse(responseCode = "200", description = "Conteggio aggiornato dei sostegni",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = VoteResponse.class)))
    @UnauthorizedApiResponse
    @DeleteMapping("/{id}/vote")
    public ResponseEntity<VoteResponse> annullaVoto(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(voteService.annullaVoto(id, currentUser.getId()));
    }

    @Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
    @Operation(
            summary = "Assegnazione del team",
            description = "Affida la segnalazione a una squadra operativa. "
                    + "Se era in attesa passa automaticamente in lavorazione.")
    @ApiResponse(responseCode = "200", description = "Team assegnato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PutMapping("/{id}/assign-team")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignTeam(
            @PathVariable Long id, @Valid @RequestBody AssignTeamRequest request) {
        return ResponseEntity.ok(reportService.assignTeam(id, request.teamId()));
    }

    @Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
    @Operation(
            summary = "Assegnazione dell'operatore",
            description = "Affida la segnalazione a un operatore, che deve appartenere al team "
                    + "già assegnato: va quindi assegnato prima il team.")
    @ApiResponse(responseCode = "200", description = "Operatore assegnato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PutMapping("/{id}/assign-operator")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignOperator(
            @PathVariable Long id, @Valid @RequestBody AssignOperatorRequest request) {
        return ResponseEntity.ok(reportService.assignOperator(id, request.operatorId()));
    }

    @Tag(name = SwaggerTags.ADMIN, description = SwaggerTags.ADMIN_DESC)
    @Operation(summary = "Modifica della priorità", description = "Imposta l'urgenza della segnalazione.")
    @ApiResponse(responseCode = "200", description = "Priorità aggiornata",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PutMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignPriority(
            @PathVariable Long id,
            @RequestBody AssignPriorityRequest request) {
        return ResponseEntity.ok(reportService.assignPriority(id, request.priority()));
    }

    @Tag(name = SwaggerTags.OPERATORE, description = SwaggerTags.OPERATORE_DESC)
    @Operation(
            summary = "Avanzamento dello stato",
            description = "Consentito all'operatore assegnato e agli amministratori. "
                    + "Una segnalazione già chiusa non cambia più stato. "
                    + "Registra la transizione nella cronologia e avvisa via email chi ha segnalato.")
    @ApiResponse(responseCode = "200", description = "Stato aggiornato",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PutMapping("/{id}/status")
    public ResponseEntity<ReportResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(reportService.changeStatus(id, request.newStatus(), request.message(), currentUser.getId()));
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(summary = "Commento a una segnalazione",
            description = "Aggiunge un intervento alla cronologia pubblica della segnalazione.")
    @ApiResponse(responseCode = "201", description = "Commento aggiunto",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UpdateResponse.class)))
    @BadRequestApiResponse
    @UnauthorizedApiResponse
    @ConflictApiResponse
    @PostMapping("/{id}/comments")
    public ResponseEntity<UpdateResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.addComment(id, request.message(), currentUser.getId()));
    }

    @Tag(name = SwaggerTags.PUBBLICO, description = SwaggerTags.PUBBLICO_DESC)
    @Operation(summary = "Cronologia di una segnalazione",
            description = "Commenti e cambi di stato in ordine cronologico. Consultabile senza autenticazione.")
    @ApiResponse(responseCode = "200", description = "Cronologia della segnalazione",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = UpdateResponse.class))))
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<UpdateResponse>> getUpdates(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getUpdates(id));
    }

    @Tag(name = SwaggerTags.CITTADINO, description = SwaggerTags.CITTADINO_DESC)
    @Operation(summary = "Caricamento di una foto",
            description = "Allega una foto alla segnalazione. Consentito al solo autore. Massimo 5 MB per file.")
    @ApiResponse(responseCode = "201", description = "Foto caricata",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReportPhotoResponse.class)))
    @UnauthorizedApiResponse
    @ForbiddenApiResponse
    @ConflictApiResponse
    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.uploadPhoto(id, file, currentUser.getId()));
    }

    @Tag(name = SwaggerTags.PUBBLICO, description = SwaggerTags.PUBBLICO_DESC)
    @Operation(summary = "Foto di una segnalazione",
            description = "Elenco delle foto allegate, con l'URL pubblico di ciascuna.")
    @ApiResponse(responseCode = "200", description = "Elenco delle foto",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = ReportPhotoResponse.class))))
    @GetMapping("/{id}/photos")
    public ResponseEntity<List<ReportPhotoResponse>> getPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getPhotos(id));
    }
}
