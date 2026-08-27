package org.civicfix.app.service;

import org.civicfix.app.dto.CreateReportRequest;
import org.civicfix.app.dto.ReportResponse;
import org.civicfix.app.model.*;
import org.civicfix.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Copre le regole di autorizzazione della segnalazione: chi può cambiare
 * stato, chi può modificarla o eliminarla, e il vincolo team/operatore.
 * Sono le regole con più conseguenze pratiche se rotte per errore — il resto
 * (CRUD semplice) vale meno la pena testare a questo livello.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UpdateRepository updateRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ReportPhotoRepository reportPhotoRepository;
    @Mock private VoteRepository voteRepository;
    @Mock private MailService mailService;

    @InjectMocks
    private ReportService reportService;

    private static final Long ID_SEGNALAZIONE = 1L;
    private static final Long ID_AUTORE = 10L;
    private static final Long ID_OPERATORE_ASSEGNATO = 20L;
    private static final Long ID_ALTRO_OPERATORE = 21L;
    private static final Long ID_ADMIN = 99L;

    private Report segnalazione;
    private User autore;
    private User operatoreAssegnato;
    private User admin;

    @BeforeEach
    void setUp() {
        autore = new User();
        autore.setId(ID_AUTORE);
        autore.setRole(Role.CITIZEN);

        operatoreAssegnato = new User();
        operatoreAssegnato.setId(ID_OPERATORE_ASSEGNATO);
        operatoreAssegnato.setRole(Role.OPERATOR);

        admin = new User();
        admin.setId(ID_ADMIN);
        admin.setRole(Role.ADMIN);

        segnalazione = new Report();
        segnalazione.setId(ID_SEGNALAZIONE);
        segnalazione.setReporter(autore);
        segnalazione.setStatus(ReportStatus.IN_PROGRESS);
        segnalazione.setAssignedOperator(operatoreAssegnato);
    }

    // --- changeStatus: chi può cambiare lo stato ---------------------------

    @Test
    void loperatoreAssegnatoPuoCambiareLoStato() {
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(userRepository.findById(ID_OPERATORE_ASSEGNATO)).thenReturn(Optional.of(operatoreAssegnato));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse esito = reportService.changeStatus(
                ID_SEGNALAZIONE, ReportStatus.RESOLVED, "Sistemato", ID_OPERATORE_ASSEGNATO);

        assertThat(esito.status()).isEqualTo(ReportStatus.RESOLVED);
        verify(mailService).inviaCambioStato(any(), eq(ReportStatus.IN_PROGRESS), eq(ReportStatus.RESOLVED), any());
    }

    @Test
    void unOperatoreDiversoDaQuelloAssegnatoNonPuoCambiareLoStato() {
        User altroOperatore = new User();
        altroOperatore.setId(ID_ALTRO_OPERATORE);
        altroOperatore.setRole(Role.OPERATOR);

        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(userRepository.findById(ID_ALTRO_OPERATORE)).thenReturn(Optional.of(altroOperatore));

        assertThatThrownBy(() -> reportService.changeStatus(
                ID_SEGNALAZIONE, ReportStatus.RESOLVED, null, ID_ALTRO_OPERATORE))
                .isInstanceOf(AccessDeniedException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void unAdminPuoCambiareLoStatoAncheSenzaEssereAssegnato() {
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(userRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(reportService.changeStatus(ID_SEGNALAZIONE, ReportStatus.REJECTED, null, ID_ADMIN).status())
                .isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void unaSegnalazioneChiusaNonPuoCambiareStatoNemmenoPerLAdmin() {
        segnalazione.setStatus(ReportStatus.RESOLVED);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));

        assertThatThrownBy(() -> reportService.changeStatus(ID_SEGNALAZIONE, ReportStatus.IN_PROGRESS, null, ID_ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chiusa");
    }

    // --- updateReport / deleteReport: solo l'autore, solo se PENDING -------

    @Test
    void soloLAutorePuoModificareLaPropriaSegnalazione() {
        segnalazione.setStatus(ReportStatus.PENDING);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));

        CreateReportRequest richiesta = new CreateReportRequest("T", "D", ReportCategory.OTHER, 1.0, 1.0, null);

        assertThatThrownBy(() -> reportService.updateReport(ID_SEGNALAZIONE, richiesta, ID_ALTRO_OPERATORE))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nonSiPuoModificareUnaSegnalazioneGiaPresaInCarico() {
        segnalazione.setStatus(ReportStatus.IN_PROGRESS);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));

        CreateReportRequest richiesta = new CreateReportRequest("T", "D", ReportCategory.OTHER, 1.0, 1.0, null);

        assertThatThrownBy(() -> reportService.updateReport(ID_SEGNALAZIONE, richiesta, ID_AUTORE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("presa in carico");
    }

    @Test
    void lAutorePuoModificareLaPropriaSegnalazioneInAttesa() {
        segnalazione.setStatus(ReportStatus.PENDING);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(voteRepository.countByReportId(ID_SEGNALAZIONE)).thenReturn(0L);

        CreateReportRequest richiesta = new CreateReportRequest(
                "Titolo nuovo", "Descrizione nuova", ReportCategory.WATER, 40.35, 18.17, "Via Test");

        ReportResponse esito = reportService.updateReport(ID_SEGNALAZIONE, richiesta, ID_AUTORE);

        assertThat(esito.title()).isEqualTo("Titolo nuovo");
        assertThat(esito.category()).isEqualTo(ReportCategory.WATER);
    }

    @Test
    void eliminareRimuoveIVotiEIFilePrimaDellaSegnalazione() {
        segnalazione.setStatus(ReportStatus.PENDING);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));

        ReportPhoto foto = new ReportPhoto();
        foto.setFilePath("una-foto.jpg");
        when(reportPhotoRepository.findByReportId(ID_SEGNALAZIONE)).thenReturn(java.util.List.of(foto));

        reportService.deleteReport(ID_SEGNALAZIONE, ID_AUTORE);

        // I voti non sono in cascata sul database: vanno rimossi esplicitamente,
        // altrimenti il vincolo di chiave esterna farebbe fallire la cancellazione.
        verify(voteRepository).deleteByReportId(ID_SEGNALAZIONE);
        verify(fileStorageService).delete("una-foto.jpg");
        verify(reportRepository).delete(segnalazione);
    }

    // --- assignOperator: l'operatore deve appartenere al team assegnato ----

    @Test
    void nonSiPuoAssegnareUnOperatoreDiUnTeamDiverso() {
        Team teamAssegnato = new Team();
        teamAssegnato.setId(1L);
        Team altroTeam = new Team();
        altroTeam.setId(2L);

        segnalazione.setAssignedTeam(teamAssegnato);
        operatoreAssegnato.setTeam(altroTeam);

        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(userRepository.findById(ID_OPERATORE_ASSEGNATO)).thenReturn(Optional.of(operatoreAssegnato));

        assertThatThrownBy(() -> reportService.assignOperator(ID_SEGNALAZIONE, ID_OPERATORE_ASSEGNATO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non appartiene al team");
    }

    @Test
    void assegnaUnOperatoreDelTeamCorretto() {
        Team team = new Team();
        team.setId(1L);
        segnalazione.setAssignedTeam(team);
        operatoreAssegnato.setTeam(team);

        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazione));
        when(userRepository.findById(ID_OPERATORE_ASSEGNATO)).thenReturn(Optional.of(operatoreAssegnato));
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse esito = reportService.assignOperator(ID_SEGNALAZIONE, ID_OPERATORE_ASSEGNATO);

        assertThat(esito.assignedOperatorUsername()).isEqualTo(operatoreAssegnato.getUsername());
    }
}
