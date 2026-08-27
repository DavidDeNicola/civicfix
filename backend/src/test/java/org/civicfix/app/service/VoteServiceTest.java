package org.civicfix.app.service;

import org.civicfix.app.dto.VoteResponse;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.ReportRepository;
import org.civicfix.app.repository.UserRepository;
import org.civicfix.app.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari puri: nessun contesto Spring, nessun database. I repository
 * sono simulati con Mockito, quindi ogni test gira in millisecondi e non
 * dipende da MySQL né da variabili d'ambiente — a differenza del test di
 * contesto completo, che richiede un database reale raggiungibile.
 */
@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VoteService voteService;

    private static final Long ID_SEGNALAZIONE = 1L;
    private static final Long ID_AUTORE = 10L;
    private static final Long ID_VOTANTE = 20L;

    private Report segnalazioneAperta;

    @BeforeEach
    void setUp() {
        User autore = new User();
        autore.setId(ID_AUTORE);

        segnalazioneAperta = new Report();
        segnalazioneAperta.setId(ID_SEGNALAZIONE);
        segnalazioneAperta.setReporter(autore);
        segnalazioneAperta.setStatus(ReportStatus.PENDING);
    }

    @Test
    void rifiutaIlVotoSullaPropriaSegnalazione() {
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazioneAperta));

        assertThatThrownBy(() -> voteService.vota(ID_SEGNALAZIONE, ID_AUTORE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hai creato tu");

        // Nessuna scrittura deve avvenire quando il voto è rifiutato.
        verify(voteRepository, never()).save(any());
    }

    @Test
    void rifiutaIlVotoSuUnaSegnalazioneChiusa() {
        segnalazioneAperta.setStatus(ReportStatus.RESOLVED);
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazioneAperta));

        assertThatThrownBy(() -> voteService.vota(ID_SEGNALAZIONE, ID_VOTANTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chiusa");
    }

    @Test
    void votoDiUnAltroUtenteVieneRegistratoESaltaSeGiaPresente() {
        User votante = new User();
        votante.setId(ID_VOTANTE);

        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazioneAperta));
        when(userRepository.findById(ID_VOTANTE)).thenReturn(Optional.of(votante));
        // Il service interroga questo metodo due volte: prima per decidere se
        // inserire il voto (non esiste ancora -> false), poi per costruire la
        // risposta (il voto è stato appena salvato -> true).
        when(voteRepository.existsByReportIdAndUserId(ID_SEGNALAZIONE, ID_VOTANTE)).thenReturn(false, true);
        when(voteRepository.countByReportId(ID_SEGNALAZIONE)).thenReturn(1L);

        VoteResponse esito = voteService.vota(ID_SEGNALAZIONE, ID_VOTANTE);

        assertThat(esito.voteCount()).isEqualTo(1L);
        assertThat(esito.votedByCurrentUser()).isTrue();
        verify(voteRepository, times(1)).save(any());
    }

    @Test
    void votareDueVolteNonCreaUnSecondoVoto() {
        when(reportRepository.findById(ID_SEGNALAZIONE)).thenReturn(Optional.of(segnalazioneAperta));
        // Il voto esiste già: il vincolo di unicità sul database è la
        // garanzia ultima, ma il service deve evitare l'inserimento a monte.
        when(voteRepository.existsByReportIdAndUserId(ID_SEGNALAZIONE, ID_VOTANTE)).thenReturn(true);
        when(voteRepository.countByReportId(ID_SEGNALAZIONE)).thenReturn(1L);

        voteService.vota(ID_SEGNALAZIONE, ID_VOTANTE);

        verify(voteRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void annullaVotoRimuoveLaRigaEAggiornaIlConteggio() {
        when(voteRepository.countByReportId(ID_SEGNALAZIONE)).thenReturn(0L);
        when(voteRepository.existsByReportIdAndUserId(ID_SEGNALAZIONE, ID_VOTANTE)).thenReturn(false);

        VoteResponse esito = voteService.annullaVoto(ID_SEGNALAZIONE, ID_VOTANTE);

        verify(voteRepository).deleteByReportIdAndUserId(ID_SEGNALAZIONE, ID_VOTANTE);
        assertThat(esito.voteCount()).isZero();
        assertThat(esito.votedByCurrentUser()).isFalse();
    }
}
