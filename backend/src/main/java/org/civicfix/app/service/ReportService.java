package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.*;
import org.civicfix.app.mapper.ReportMapper;
import org.civicfix.app.mapper.ReportPhotoMapper;
import org.civicfix.app.mapper.UpdateMapper;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.civicfix.app.model.ReportPriority;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Cuore del dominio: crea, modifica, assegna e cambia stato alle segnalazioni.
 * Le regole di autorizzazione (solo l'autore, solo se PENDING, solo l'operatore
 * assegnato o un admin) vivono qui, non nel controller.
 */

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UpdateRepository updateRepository;
    private final FileStorageService fileStorageService;
    private final ReportPhotoRepository reportPhotoRepository;
    private final VoteRepository voteRepository;
    private final MailService mailService;
    private final ReportMapper reportMapper;
    private final UpdateMapper updateMapper;
    private final ReportPhotoMapper reportPhotoMapper;

    public ReportResponse createReport(CreateReportRequest request, Long reporterId) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Report report = reportMapper.toEntity(request, reporter);

        // Appena creata non può avere sostegni: qui lo zero è un dato di fatto,
        // non un valore mancante.
        return reportMapper.toResponse(reportRepository.save(report), 0L, false);
    }

    public ReportResponse getById(Long id, Long currentUserId) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));

        long voti = voteRepository.countByReportId(id);
        boolean votata = currentUserId != null && voteRepository.existsByReportIdAndUserId(id, currentUserId);

        return reportMapper.toResponse(report, voti, votata);
    }

    public PagedResponse<ReportResponse> searchReports(
            List<ReportCategory> categories,
            List<ReportStatus> statuses,
            String titolo,
            LocalDate da,
            LocalDate a,
            Double lat,
            Double lng,
            Double radiusKm,
            int page,
            int size,
            Long currentUserId) {

        Specification<Report> spec = Specification
                .where(ReportSpecifications.hasCategoryIn(categories))
                .and(ReportSpecifications.hasStatusIn(statuses))
                .and(ReportSpecifications.titleContains(titolo))
                .and(ReportSpecifications.createdBetween(da, a))
                .and(ReportSpecifications.nearLocation(lat, lng, radiusKm));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Report> pagina = reportRepository.findAll(spec, pageable);
        List<Long> ids = pagina.getContent().stream().map(Report::getId).toList();

        // Due query per l'intera pagina invece di due per riga.
        Map<Long, Long> conteggi = ids.isEmpty() ? Map.of()
                : voteRepository.contaPerSegnalazioni(ids).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        Set<Long> votate = (currentUserId == null || ids.isEmpty()) ? Set.of()
                : new HashSet<>(voteRepository.segnalazioniVotateDa(currentUserId, ids));

        Page<ReportResponse> result = pagina.map(report -> reportMapper.toResponse(
                report,
                conteggi.getOrDefault(report.getId(), 0L),
                votate.contains(report.getId())));

        return PagedResponse.from(result);
    }

    /**
     * Modifica una segnalazione. È concesso solo a chi l'ha aperta e solo
     * finché è in attesa: appena viene presa in carico, team e operatore
     * stanno già lavorando su quanto descritto, e cambiarlo sotto di loro
     * renderebbe incoerente la cronologia.
     */
    @Transactional
    public ReportResponse updateReport(Long reportId, CreateReportRequest request, Long currentUserId) {
        Report report = caricaModificabile(reportId, currentUserId);
        reportMapper.applicaModifiche(report, request);

        Report salvata = reportRepository.save(report);
        // Chi modifica è l'autore, e l'autore non può sostenere la propria
        // segnalazione (regola applicata da VoteService): quindi mai votata.
        return reportMapper.toResponse(salvata, voteRepository.countByReportId(reportId), false);
    }

    @Transactional
    public void deleteReport(Long reportId, Long currentUserId) {
        Report report = caricaModificabile(reportId, currentUserId);

        // I voti non sono mappati come figli della segnalazione: senza questa
        // rimozione resterebbero righe orfane e il vincolo di chiave esterna
        // farebbe fallire la cancellazione.
        voteRepository.deleteByReportId(reportId);

        // Le foto sono in cascata sul database, ma i file no.
        for (ReportPhoto foto : reportPhotoRepository.findByReportId(reportId)) {
            fileStorageService.delete(foto.getFilePath());
        }

        reportRepository.delete(report);
    }

    /**
     * Risposta per le operazioni lato gestione (assegnazione team/operatore,
     * priorità). Il conteggio dei sostegni va riletto: la dashboard sostituisce
     * la segnalazione in elenco con questa risposta, quindi restituire zero la
     * farebbe apparire senza sostegni fino al ricaricamento della pagina.
     * Il flag "votata dall'utente corrente" resta invece false perché qui
     * l'utente è un amministratore, non il cittadino a cui serve quel dato.
     */
    private ReportResponse rispostaDiGestione(Report report) {
        return reportMapper.toResponse(report, voteRepository.countByReportId(report.getId()), false);
    }

    private Report caricaModificabile(Long reportId, Long currentUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));

        if (!report.getReporter().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Puoi intervenire solo sulle tue segnalazioni");
        }
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalArgumentException(
                    "La segnalazione è già stata presa in carico e non può più essere modificata o eliminata");
        }

        return report;
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

        return rispostaDiGestione(reportRepository.save(report));
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
        return rispostaDiGestione(reportRepository.save(report));
    }

    public ReportResponse changeStatus(Long reportId, ReportStatus newStatus, String message, Long currentUserId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new IllegalArgumentException("Una segnalazione chiusa non può cambiare stato");
        }
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

        // Avvisa chi ha segnalato: è l'unico modo che ha di sapere che
        // qualcosa si è mosso senza tornare a controllare da solo.
        mailService.inviaCambioStato(report, oldStatus, newStatus, message);

        // Qui l'utente corrente è noto (operatore o admin) e può aver sostenuto
        // la segnalazione in passato: il flag si può valorizzare davvero.
        return reportMapper.toResponse(
                report,
                voteRepository.countByReportId(reportId),
                voteRepository.existsByReportIdAndUserId(reportId, currentUserId));
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

        return updateMapper.toResponse(updateRepository.save(update));
    }

    public List<UpdateResponse> getUpdates(Long reportId) {
        return updateRepository.findByReportIdOrderByCreatedAtAsc(reportId)
                .stream().map(updateMapper::toResponse).toList();
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

        return reportPhotoMapper.toResponse(reportPhotoRepository.save(photo));
    }

    public List<ReportPhotoResponse> getPhotos(Long reportId) {
        return reportPhotoRepository.findByReportId(reportId)
                .stream().map(reportPhotoMapper::toResponse).toList();
    }

    public ReportResponse assignPriority(Long reportId, ReportPriority priority) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));
        report.setPriority(priority);
        reportRepository.save(report);
        return rispostaDiGestione(report);
    }
}

 