package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.VoteResponse;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.User;
import org.civicfix.app.model.Vote;
import org.civicfix.app.repository.ReportRepository;
import org.civicfix.app.repository.UserRepository;
import org.civicfix.app.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    /**
     * Aggiunge il sostegno dell'utente alla segnalazione. Il vincolo di unicità
     * su (report, user) impedisce comunque il doppio voto a livello di
     * database; qui si intercetta prima per rispondere in modo sensato.
     */
    @Transactional
    public VoteResponse vota(Long reportId, Long userId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Segnalazione non trovata"));

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.REJECTED) {
            throw new IllegalArgumentException("Una segnalazione chiusa non può più essere sostenuta");
        }

        // Sostenere la propria segnalazione gonfierebbe il conteggio senza
        // aggiungere l'informazione che serve: quante altre persone la sentono.
        if (report.getReporter().getId().equals(userId)) {
            throw new IllegalArgumentException("Non puoi sostenere una segnalazione che hai creato tu");
        }

        if (!voteRepository.existsByReportIdAndUserId(reportId, userId)) {
            User utente = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

            Vote voto = new Vote();
            voto.setReport(report);
            voto.setUser(utente);
            voteRepository.save(voto);
        }

        return stato(reportId, userId);
    }

    @Transactional
    public VoteResponse annullaVoto(Long reportId, Long userId) {
        voteRepository.deleteByReportIdAndUserId(reportId, userId);
        return stato(reportId, userId);
    }

    @Transactional(readOnly = true)
    public VoteResponse stato(Long reportId, Long userId) {
        long totale = voteRepository.countByReportId(reportId);
        boolean votata = userId != null && voteRepository.existsByReportIdAndUserId(reportId, userId);
        return new VoteResponse(totale, votata);
    }
}
