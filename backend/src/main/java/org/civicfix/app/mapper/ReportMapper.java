package org.civicfix.app.mapper;

import org.civicfix.app.dto.CreateReportRequest;
import org.civicfix.app.dto.ReportResponse;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.User;
import org.springframework.stereotype.Component;

/**
 * Conversione fra l'entità Report e i suoi DTO.
 *
 * I dati di voto non stanno sull'entità (i voti sono una tabella a parte, letta
 * con query aggregate): vanno quindi passati dal chiamante. Non esiste di
 * proposito una variante che li ometta — quando c'era, i punti in cui non
 * venivano passati restituivano zero sostegni senza che si notasse.
 */
@Component
public class ReportMapper {

    public ReportResponse toResponse(Report report, long voteCount, boolean votedByCurrentUser) {
        return new ReportResponse(
                report.getId(),
                report.getTitle(),
                report.getDescription(),
                report.getCategory(),
                report.getStatus(),
                report.getPriority(),
                report.getLatitude(),
                report.getLongitude(),
                report.getAddress(),
                report.getReporter().getUsername(),
                report.getAssignedTeam() != null ? report.getAssignedTeam().getName() : null,
                report.getAssignedOperator() != null ? report.getAssignedOperator().getUsername() : null,
                voteCount,
                votedByCurrentUser,
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    public Report toEntity(CreateReportRequest request, User reporter) {
        Report report = new Report();
        applicaModifiche(report, request);
        report.setReporter(reporter);
        // status e priority restano ai valori di default dell'entità
        return report;
    }

    /** Ricopia sulla segnalazione i soli campi che l'autore può modificare. */
    public void applicaModifiche(Report report, CreateReportRequest request) {
        report.setTitle(request.title());
        report.setDescription(request.description());
        report.setCategory(request.category());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setAddress(request.address());
    }
}
