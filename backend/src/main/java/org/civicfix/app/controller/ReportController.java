package org.civicfix.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.*;
import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.security.CustomUserDetails;
import org.civicfix.app.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportResponse> create(
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.createReport(request, currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ReportResponse>> search(
            @RequestParam(required = false) ReportCategory category,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(reportService.searchReports(category, status, lat, lng, radiusKm, page, size));
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
}
