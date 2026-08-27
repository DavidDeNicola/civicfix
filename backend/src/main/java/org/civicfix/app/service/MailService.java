package org.civicfix.app.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.civicfix.app.model.Report;
import org.civicfix.app.model.ReportStatus;
import org.civicfix.app.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private static final List<String> VARIABILI_SMTP = List.of("MAIL_USERNAME", "MAIL_PASSWORD");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:}")
    private String mittente;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Verifica all'avvio se l'invio email è attivo e, quando non lo è, spiega
     * il motivo invece di limitarsi a segnalarlo al primo invio. Il caso più
     * insidioso è una variabile definita con spazi nel nome (capita incollando
     * un elenco separato da ";" nel campo "Environment variables" dell'IDE):
     * la variabile risulta presente nel processo ma con un nome che non
     * corrisponde, quindi resta invisibile alla configurazione.
     */
    @PostConstruct
    void verificaConfigurazione() {
        if (StringUtils.hasText(mailUsername)) {
            log.info("SMTP attivo: le email di recupero password verranno inviate.");
            return;
        }

        log.warn("SMTP non attivo: 'spring.mail.username' risulta vuoto. "
                + "I link di recupero restano comunque leggibili in questi log.");

        for (String atteso : VARIABILI_SMTP) {
            if (StringUtils.hasText(System.getenv(atteso))) {
                log.warn("  - {}: presente nell'ambiente ma non applicata alla configurazione.", atteso);
                continue;
            }

            // Nessun valore stampato: solo il nome così com'è realmente definito.
            String simile = System.getenv().keySet().stream()
                    .filter(chiave -> !chiave.equals(atteso) && chiave.trim().equals(atteso))
                    .findFirst()
                    .orElse(null);

            if (simile != null) {
                log.warn("  - {}: definita con un nome errato -> '{}' (contiene spazi). "
                        + "Correggi il nome nella configurazione di avvio.", atteso, simile);
            } else {
                log.warn("  - {}: non definita nell'ambiente del processo.", atteso);
            }
        }
    }

    private static final Map<ReportStatus, String> ETICHETTE_STATO = Map.of(
            ReportStatus.PENDING, "In attesa",
            ReportStatus.IN_PROGRESS, "In corso",
            ReportStatus.RESOLVED, "Risolta",
            ReportStatus.REJECTED, "Respinta"
    );

    /**
     * Avvisa chi ha aperto la segnalazione che lo stato è cambiato. Come per il
     * recupero password, un problema di invio non deve far fallire l'operazione
     * che l'ha innescata: il cambio di stato è già stato salvato.
     */
    public void inviaCambioStato(Report report, ReportStatus vecchio, ReportStatus nuovo, String messaggio) {
        User destinatario = report.getReporter();
        if (destinatario == null || !StringUtils.hasText(destinatario.getEmail())) {
            return;
        }

        String corpo = """
                Ciao %s,

                la tua segnalazione "%s" è passata da "%s" a "%s".
                """.formatted(
                        destinatario.getFullName(),
                        report.getTitle(),
                        etichetta(vecchio),
                        etichetta(nuovo));

        if (StringUtils.hasText(messaggio)) {
            corpo += "\nNota di chi l'ha aggiornata:\n%s\n".formatted(messaggio);
        }

        corpo += """

                Puoi vedere la cronologia completa qui:
                %s/reports/%d

                — CivicFix
                """.formatted(frontendUrl, report.getId());

        invia(destinatario.getEmail(), "CivicFix - Aggiornamento della tua segnalazione", corpo);
    }

    private String etichetta(ReportStatus stato) {
        return stato == null ? "—" : ETICHETTE_STATO.getOrDefault(stato, stato.name());
    }

    public void inviaLinkReset(String destinatario, String nomeCompleto, String link, int validitaMinuti) {
        String corpo = """
                Ciao %s,

                abbiamo ricevuto una richiesta di reimpostazione della password del tuo account CivicFix.

                Apri questo link per scegliere una nuova password:
                %s

                Il link scade tra %d minuti e può essere usato una sola volta.

                Se non hai richiesto tu il recupero puoi ignorare questa email:
                la tua password attuale resta valida.

                — CivicFix
                """.formatted(nomeCompleto, link, validitaMinuti);

        invia(destinatario, "CivicFix - Recupero password", corpo);
    }

    /**
     * Punto unico di invio. Un errore SMTP viene registrato ma non propagato:
     * l'operazione che ha innescato l'email è già stata completata e non deve
     * fallire perché il server di posta è irraggiungibile.
     */
    private void invia(String destinatario, String oggetto, String corpo) {
        if (!StringUtils.hasText(mailUsername)) {
            log.warn("SMTP non attivo: email \"{}\" non inviata a {}. "
                    + "Il motivo è indicato nei messaggi emessi all'avvio dell'applicazione.",
                    oggetto, destinatario);
            return;
        }

        SimpleMailMessage messaggio = new SimpleMailMessage();
        messaggio.setFrom(StringUtils.hasText(mittente) ? mittente : mailUsername);
        messaggio.setTo(destinatario);
        messaggio.setSubject(oggetto);
        messaggio.setText(corpo);

        try {
            mailSender.send(messaggio);
            log.info("Email \"{}\" inviata a {}", oggetto, destinatario);
        } catch (Exception ex) {
            log.error("Invio dell'email \"{}\" a {} non riuscito: {}", oggetto, destinatario, ex.getMessage());
        }
    }
}
