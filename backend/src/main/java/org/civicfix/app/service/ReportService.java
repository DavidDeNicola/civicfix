package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.*;
import org.civicfix.app.model.*;
import org.civicfix.app.repository.*;
import org.civicfix.app.repository.specification.ReportSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UpdateRepository updateRepository;
    private final FileStorageService fileStorageService;
    private final ReportPhotoRepository reportPhotoRepository;

    public ReportResponse createReport(CreateReportRequest request, Long reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Report report = new Report();
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setCategory(request.category());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setAddress(request.address());
        report.setReporter(reporter);
        // status resta PENDING di default, come impostato nell'entità

        return ReportResponse.from(reportRepository.save(report));
    }

    public ReportResponse getById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        return ReportResponse.from(report);
    }

    public PagedResponse<ReportResponse> searchReports(
            ReportCategory category,
            ReportStatus status,
            Double lat,
            Double lng,
            Double radiusKm,
            int page,
            int size) {

        Specification<Report> spec = Specification
                .where(ReportSpecifications.hasCategory(category))
                .and(ReportSpecifications.hasStatus(status))
                .and(ReportSpecifications.nearLocation(lat, lng, radiusKm));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReportResponse> result = reportRepository.findAll(spec, pageable)
                .map(ReportResponse::from);

        return PagedResponse.from(result);
    }

    public ReportResponse assignTeam(Long reportId, Long teamId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team non trovato"));

        report.setAssignedTeam(team);
        if (report.getStatus() == ReportStatus.PENDING) {
            report.setStatus(ReportStatus.IN_PROGRESS);
        }

        return ReportResponse.from(reportRepository.save(report));
    }

    public ReportResponse assignOperator(Long reportId, Long operatorId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("Operatore non trovato"));

        if (operator.getRole() != Role.OPERATOR) {
            throw new IllegalArgumentException("L'utente selezionato non è un operatore");
        }
        if (report.getAssignedTeam() == null || !report.getAssignedTeam().equals(operator.getTeam())) {
            throw new IllegalArgumentException("L'operatore non appartiene al team assegnato a questa segnalazione");
        }

        report.setAssignedOperator(operator);
        return ReportResponse.from(reportRepository.save(report));
    }

    public ReportResponse changeStatus(Long reportId, ReportStatus newStatus, String message, Long currentUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        boolean isAssignedOperator = report.getAssignedOperator() != null
                && report.getAssignedOperator().getId().equals(currentUserId);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAssignedOperator && !isAdmin) {
            throw new AccessDeniedException("Solo l'operatore assegnato o un admin possono modificare lo stato");
        }

        ReportStatus oldStatus = report.getStatus();
        report.setStatus(newStatus);
        reportRepository.save(report);

        Update update = new Update();
        update.setReport(report);
        update.setAuthor(currentUser);
        update.setType(UpdateType.STATUS_CHANGE);
        update.setMessage(message);
        update.setOldStatus(oldStatus);
        update.setNewStatus(newStatus);
        updateRepository.save(update);

        return ReportResponse.from(report);
    }

    public UpdateResponse addComment(Long reportId, String message, Long currentUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Update update = new Update();
        update.setReport(report);
        update.setAuthor(author);
        update.setType(UpdateType.COMMENT);
        update.setMessage(message);

        return UpdateResponse.from(updateRepository.save(update));
    }

    public List<UpdateResponse> getUpdates(Long reportId) {
        return updateRepository.findByReportIdOrderByCreatedAtAsc(reportId)
                .stream().map(UpdateResponse::from).toList();
    }

    public ReportPhotoResponse uploadPhoto(Long reportId, MultipartFile file, Long currentUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));

        if (!report.getReporter().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Solo l'autore della segnalazione può caricare foto");
        }

        String filename = fileStorageService.store(file);

        ReportPhoto photo = new ReportPhoto();
        photo.setReport(report);
        photo.setFilePath(filename);

        return ReportPhotoResponse.from(reportPhotoRepository.save(photo));
    }

    public List<ReportPhotoResponse> getPhotos(Long reportId) {
        return reportPhotoRepository.findByReportId(reportId)
                .stream().map(ReportPhotoResponse::from).toList();
    }
}

 