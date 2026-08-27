package org.civicfix.app.service;

import org.civicfix.app.dto.StatisticsResponse;
import org.civicfix.app.model.*;
import org.civicfix.app.repository.ReportRepository;
import org.civicfix.app.repository.UpdateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UpdateRepository updateRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Team team;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);
        team.setName("Squadra Strade");
        team.setCategory(ReportCategory.VIABILITY);
    }

    private Report segnalazione(ReportStatus stato, ReportCategory categoria, ReportPriority priorita, Team teamAssegnato) {
        Report report = new Report();
        report.setStatus(stato);
        report.setCategory(categoria);
        report.setPriority(priorita);
        report.setAssignedTeam(teamAssegnato);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    @Test
    void raggruppaLeSegnalazioniPerStatoCategoriaEPriorita() {
        List<Report> segnalazioni = List.of(
                segnalazione(ReportStatus.PENDING, ReportCategory.VIABILITY, ReportPriority.NORMAL, null),
                segnalazione(ReportStatus.PENDING, ReportCategory.WASTE, ReportPriority.HIGH, null),
                segnalazione(ReportStatus.RESOLVED, ReportCategory.VIABILITY, ReportPriority.LOW, team)
        );
        when(reportRepository.findAll()).thenReturn(segnalazioni);
        when(updateRepository.findByNewStatus(ReportStatus.RESOLVED)).thenReturn(List.of());

        StatisticsResponse esito = statisticsService.getStatistics();

        assertThat(esito.totalReports()).isEqualTo(3);
        assertThat(esito.byStatus()).containsEntry("PENDING", 2L).containsEntry("RESOLVED", 1L);
        assertThat(esito.byCategory()).containsEntry("VIABILITY", 2L).containsEntry("WASTE", 1L);
        assertThat(esito.byPriority()).containsEntry("NORMAL", 1L).containsEntry("HIGH", 1L).containsEntry("LOW", 1L);
    }

    @Test
    void andamentoMensileContieneSempreDodiciMesiAncheSenzaSegnalazioni() {
        when(reportRepository.findAll()).thenReturn(List.of());
        when(updateRepository.findByNewStatus(ReportStatus.RESOLVED)).thenReturn(List.of());

        StatisticsResponse esito = statisticsService.getStatistics();

        assertThat(esito.reportsPerMonth()).hasSize(12);
        assertThat(esito.reportsPerMonth()).allMatch(m -> m.count() == 0);
    }

    @Test
    void mediaTempoRisoluzioneNullaSenzaSegnalazioniRisolte() {
        when(reportRepository.findAll()).thenReturn(List.of());
        when(updateRepository.findByNewStatus(ReportStatus.RESOLVED)).thenReturn(List.of());

        StatisticsResponse esito = statisticsService.getStatistics();

        assertThat(esito.averageResolutionHours()).isNull();
    }

    @Test
    void calcolaLaMediaDelleOreDiRisoluzioneDalloStoricoAggiornamenti() {
        Report report = segnalazione(ReportStatus.RESOLVED, ReportCategory.VIABILITY, ReportPriority.NORMAL, team);
        report.setCreatedAt(LocalDateTime.now().minusHours(10));

        Update risoluzione = new Update();
        risoluzione.setReport(report);
        risoluzione.setNewStatus(ReportStatus.RESOLVED);
        risoluzione.setCreatedAt(report.getCreatedAt().plusHours(4));

        when(reportRepository.findAll()).thenReturn(List.of(report));
        when(updateRepository.findByNewStatus(ReportStatus.RESOLVED)).thenReturn(List.of(risoluzione));

        StatisticsResponse esito = statisticsService.getStatistics();

        assertThat(esito.averageResolutionHours()).isEqualTo(4.0);
    }

    @Test
    void classificaITeamPerSegnalazioniRisolteEscludendoLeAltre() {
        Team altroTeam = new Team();
        altroTeam.setId(2L);
        altroTeam.setName("Squadra Verde");
        altroTeam.setCategory(ReportCategory.GREEN_AREAS);

        List<Report> segnalazioni = List.of(
                segnalazione(ReportStatus.RESOLVED, ReportCategory.VIABILITY, ReportPriority.NORMAL, team),
                segnalazione(ReportStatus.RESOLVED, ReportCategory.VIABILITY, ReportPriority.NORMAL, team),
                segnalazione(ReportStatus.RESOLVED, ReportCategory.GREEN_AREAS, ReportPriority.NORMAL, altroTeam),
                // In lavorazione: non deve contare come "risolta" per nessun team.
                segnalazione(ReportStatus.IN_PROGRESS, ReportCategory.VIABILITY, ReportPriority.NORMAL, team)
        );
        when(reportRepository.findAll()).thenReturn(segnalazioni);
        when(updateRepository.findByNewStatus(ReportStatus.RESOLVED)).thenReturn(List.of());

        StatisticsResponse esito = statisticsService.getStatistics();

        assertThat(esito.topTeams()).hasSize(2);
        assertThat(esito.topTeams().get(0).teamName()).isEqualTo("Squadra Strade");
        assertThat(esito.topTeams().get(0).resolvedCount()).isEqualTo(2L);
    }
}
