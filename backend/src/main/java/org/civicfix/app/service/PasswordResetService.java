package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.civicfix.app.model.User;
import org.civicfix.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    /** Finestra volutamente breve: un link di reset è una credenziale a tutti gli effetti. */
    private static final int VALIDITA_MINUTI = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Genera un token di reset per l'email indicata, lo invia via mail e lo
     * stampa nei log. Non segnala in alcun modo se l'email esiste: il
     * chiamante riceve sempre la stessa risposta, così l'endpoint non può
     * essere usato per scoprire quali indirizzi sono registrati.
     */
    @Transactional
    public void richiediReset(String email) {
        Optional<User> utenteOpt = userRepository.findByEmail(email);

        if (utenteOpt.isEmpty()) {
            log.info("Richiesta di reset password per un'email non registrata: {}", email);
            return;
        }

        User utente = utenteOpt.get();

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        utente.setResetTokenHash(hash(token));
        utente.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(VALIDITA_MINUTI));
        userRepository.save(utente);

        String link = frontendUrl + "/reset-password?token=" + token;

        // Stampato sempre, anche quando la mail parte: serve a recuperare il
        // link subito in sviluppo senza aprire la casella di posta.
        log.info("""

                ┌──────────────────────────────────────────────────────────────
                │ RECUPERO PASSWORD - {}
                │ Token: {}
                │ Link:  {}
                │ Scade tra {} minuti
                └──────────────────────────────────────────────────────────────
                """, utente.getUsername(), token, link, VALIDITA_MINUTI);

        mailService.inviaLinkReset(utente.getEmail(), utente.getFullName(), link, VALIDITA_MINUTI);
    }

    /**
     * Consuma il token e imposta la nuova password.
     *
     * @throws IllegalArgumentException se il token è sconosciuto o scaduto
     */
    @Transactional
    public void reimpostaPassword(String token, String nuovaPassword) {
        User utente = userRepository.findByResetTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Link di recupero non valido o già utilizzato."));

        if (utente.getResetTokenExpiresAt() == null
                || utente.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            // Il token scaduto viene comunque rimosso, così non resta a bagnomaria.
            utente.setResetTokenHash(null);
            utente.setResetTokenExpiresAt(null);
            userRepository.save(utente);
            throw new IllegalArgumentException("Il link di recupero è scaduto. Richiedine uno nuovo.");
        }

        utente.setPasswordHash(passwordEncoder.encode(nuovaPassword));
        // Token monouso: azzerato subito dopo l'uso.
        utente.setResetTokenHash(null);
        utente.setResetTokenExpiresAt(null);
        userRepository.save(utente);

        log.info("Password reimpostata per l'utente {}", utente.getUsername());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}
