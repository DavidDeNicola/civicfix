package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.StatisticsResponse;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.Update;
import org.civicfix.app.repository.ReportRepository;
import org.civicfix.app.repository.UpdateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final int MESI_ANDAMENTO = 12;
    private static final int TOP_TEAM_LIMITE = 5;

    private final ReportRepository reportRepository;
    private final UpdateRepository updateRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        List<Report> reports = reportRepository.findAll();

        return new StatisticsResponse(
                reports.size(),
                conteggioPer(reports, r -> r.getStatus().name()),
                conteggioPer(reports, r -> r.getCategory().name()),
                conteggioPer(reports, r -> r.getPriority().name()),
                andamentoMensile(reports),
                tempoMedioRisoluzioneOre(),
                teamPiuAttivi(reports)
        );
    }

    private Map<String, Long> conteggioPer(List<Report> reports, java.util.function.Function<Report, String> chiave) {
        return reports.stream().collect(Collectors.groupingBy(chiave, Collectors.counting()));
    }

    /**
     * Genera sempre gli ultimi 12 mesi, compresi quelli senza segnalazioni:
     * un grafico a cui mancano dei punti nel mezzo è più fuorviante di uno a
     * zero.
     */
    private List<StatisticsResponse.MonthlyCount> andamentoMensile(List<Report> reports) {
        Map<YearMonth, Long> perMese = reports.stream().collect(Collectors.groupingBy(
                r -> YearMonth.from(r.getCreatedAt()), Collectors.counting()));

        YearMonth meseCorrente = YearMonth.now();
        Map<YearMonth, Long> ultimiMesi = new LinkedHashMap<>();
        for (int i = MESI_ANDAMENTO - 1; i >= 0; i--) {
            YearMonth mese = meseCorrente.minusMonths(i);
            ultimiMesi.put(mese, perMese.getOrDefault(mese, 0L));
        }

        return ultimiMesi.entrySet().stream()
                .map(e -> new StatisticsResponse.MonthlyCount(etichettaMese(e.getKey()), e.getValue()))
                .toList();
    }

    private String etichettaMese(YearMonth mese) {
        String nomeMese = mese.getMonth().getDisplayName(TextStyle.SHORT, Locale.ITALIAN);
        return nomeMese + " " + mese.getYear();
    }

    /**
     * Media calcolata sulle transizioni di stato verso RESOLVED registrate
     * nello storico aggiornamenti, non sull'ultimo updatedAt della
     * segnalazione: quel campo cambia anche per modifiche successive alla
     * risoluzione (es. una riassegnazione), quindi non è affidabile.
     */
    private Double tempoMedioRisoluzioneOre() {
        List<Update> risoluzioni = updateRepository.findByNewStatus(ReportStatus.RESOLVED);
        if (risoluzioni.isEmpty()) {
            return null;
        }

        double totaleOre = risoluzioni.stream()
                .mapToDouble(u -> Duration.between(u.getReport().getCreatedAt(), u.getCreatedAt()).toMinutes() / 60.0)
                .sum();

        return totaleOre / risoluzioni.size();
    }

    private List<StatisticsResponse.TeamCount> teamPiuAttivi(List<Report> reports) {
        return reports.stream()
                .filter(r -> r.getStatus() == ReportStatus.RESOLVED && r.getAssignedTeam() != null)
                .collect(Collectors.groupingBy(r -> r.getAssignedTeam().getName(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new StatisticsResponse.TeamCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(StatisticsResponse.TeamCount::resolvedCount).reversed())
                .limit(TOP_TEAM_LIMITE)
                .toList();
    }
}
