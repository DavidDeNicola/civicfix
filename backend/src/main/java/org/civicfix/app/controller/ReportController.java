package org.civicfix.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(request, currentUser.getId()));
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<ReportResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(reportService.updateReport(id, request, currentUser.getId()));
    }

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

    @PostMapping("/{id}/vote")
    public ResponseEntity<VoteResponse> vota(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(voteService.vota(id, currentUser.getId()));
    }

    @DeleteMapping("/{id}/vote")
    public ResponseEntity<VoteResponse> annullaVoto(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(voteService.annullaVoto(id, currentUser.getId()));
    }

    @PutMapping("/{id}/assign-team")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignTeam(
            @PathVariable Long id, @Valid @RequestBody AssignTeamRequest request) {
        return ResponseEntity.ok(reportService.assignTeam(id, request.teamId()));
    }

    @PutMapping("/{id}/assign-operator")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignOperator(
            @PathVariable Long id, @Valid @RequestBody AssignOperatorRequest request) {
        return ResponseEntity.ok(reportService.assignOperator(id, request.operatorId()));
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> assignPriority(
            @PathVariable Long id,
            @RequestBody AssignPriorityRequest request) {
        return ResponseEntity.ok(reportService.assignPriority(id, request.priority()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReportResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(reportService.changeStatus(id, request.newStatus(), request.message(), currentUser.getId()));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<UpdateResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.addComment(id, request.message(), currentUser.getId()));
    }

    @GetMapping("/{id}/updates")
    public ResponseEntity<List<UpdateResponse>> getUpdates(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getUpdates(id));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportPhotoResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.uploadPhoto(id, file, currentUser.getId()));
    }

    @GetMapping("/{id}/photos")
    public ResponseEntity<List<ReportPhotoResponse>> getPhotos(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getPhotos(id));
    }
}
