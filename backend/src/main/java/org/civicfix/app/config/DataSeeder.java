package org.civicfix.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.civicfix.app.model.*;
import org.civicfix.app.repository.ReportRepository;
import org.civicfix.app.repository.TeamRepository;
import org.civicfix.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Popola un ambiente di sviluppo vuoto con dati realistici: una ventina di
 * cittadini, una decina di operatori distribuiti sui team, un team per
 * categoria e un buon numero di segnalazioni sparse per stato e priorità.
 * Utile per provare filtri, mappa, paginazione e dashboard senza doverli
 * riempire a mano ogni volta che il database viene ricreato.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;
    @Value("${admin.email}")
    private String adminEmail;
    @Value("${admin.password}")
    private String adminPassword;

    /** Seed fisso: dati diversi a ogni riavvio renderebbero i test manuali meno ripetibili. */
    private final Random random = new Random(42);

    // Centro di Lecce; le coordinate dei report vengono sparse attorno a questo punto.
    private static final double LAT_BASE = 40.3515;
    private static final double LNG_BASE = 18.1750;

    private static final String[] NOMI = {
            "Marco", "Giulia", "Luca", "Anna", "Francesco", "Sara", "Matteo", "Chiara",
            "Alessandro", "Elena", "Davide", "Francesca", "Simone", "Valentina", "Andrea",
            "Martina", "Stefano", "Laura", "Roberto", "Ilaria", "Paolo", "Federica",
            "Antonio", "Silvia"
    };
    private static final String[] COGNOMI = {
            "Rossi", "Russo", "Ferrari", "Esposito", "Bianchi", "Romano", "Colombo",
            "Ricci", "Marino", "Greco", "Bruno", "Gallo", "Conti", "De Luca", "Costa",
            "Giordano", "Mancini", "Rizzo", "Lombardi", "Moretti", "Fontana", "Serra"
    };

    private record ModelloSegnalazione(
            String titolo, String descrizione, ReportCategory categoria, double dLat, double dLng, String via) {
    }

    /** Titoli e descrizioni verosimili, uno per categoria così ogni team ha lavoro assegnabile. */
    private static final ModelloSegnalazione[] MODELLI = {
            new ModelloSegnalazione("Buca pericolosa", "Buca profonda vicino al marciapiede", ReportCategory.VIABILITY, 0.002, -0.003, "Via Roma"),
            new ModelloSegnalazione("Asfalto dissestato", "Tratto di strada con crepe estese", ReportCategory.VIABILITY, -0.010, 0.006, "Viale Gallipoli"),
            new ModelloSegnalazione("Segnaletica scomparsa", "Strisce pedonali non più visibili", ReportCategory.VIABILITY, 0.015, 0.010, "Via Cavallotti"),
            new ModelloSegnalazione("Lampione spento", "Luce non funzionante da una settimana", ReportCategory.LIGHTING, 0.004, -0.006, "Via Palmieri"),
            new ModelloSegnalazione("Illuminazione intermittente", "Il lampione lampeggia di continuo", ReportCategory.LIGHTING, -0.006, 0.012, "Via Trinchese"),
            new ModelloSegnalazione("Zona buia la sera", "Assenza totale di illuminazione pubblica", ReportCategory.LIGHTING, 0.011, -0.014, "Via del Mare"),
            new ModelloSegnalazione("Cassonetto pieno", "Rifiuti accumulati fuori dal cassonetto", ReportCategory.WASTE, -0.003, 0.004, "Piazza Mazzini"),
            new ModelloSegnalazione("Rifiuti abbandonati", "Sacchi lasciati sul marciapiede", ReportCategory.WASTE, 0.008, 0.002, "Via Adriatica"),
            new ModelloSegnalazione("Raccolta saltata", "Il ritiro non passa da tre giorni", ReportCategory.WASTE, -0.012, -0.008, "Via Merine"),
            new ModelloSegnalazione("Aiuola incolta", "Erba alta e rami secchi", ReportCategory.GREEN_AREAS, 0.006, 0.015, "Giardini Pubblici"),
            new ModelloSegnalazione("Albero pericolante", "Ramo spezzato sospeso sul sentiero", ReportCategory.GREEN_AREAS, -0.009, 0.009, "Villa Comunale"),
            new ModelloSegnalazione("Panchine rotte", "Due panchine del parco danneggiate", ReportCategory.GREEN_AREAS, 0.013, -0.005, "Parco Belloluogo"),
            new ModelloSegnalazione("Perdita idrica", "Acqua che sgorga dal marciapiede", ReportCategory.WATER, 0.005, -0.011, "Viale Otranto"),
            new ModelloSegnalazione("Tombino otturato", "Ristagno d'acqua dopo la pioggia", ReportCategory.WATER, -0.007, -0.002, "Via Taranto"),
            new ModelloSegnalazione("Pressione idrica bassa", "Da giorni l'acqua esce debole", ReportCategory.WATER, 0.009, 0.007, "Via Lecce-Torre"),
            new ModelloSegnalazione("Cartello caduto", "Segnale stradale a terra dopo il vento", ReportCategory.OTHER, -0.004, 0.011, "Via San Nicola"),
            new ModelloSegnalazione("Muretto danneggiato", "Muro di contenimento con crepe visibili", ReportCategory.OTHER, 0.007, -0.009, "Via Napoli"),
    };

    @Override
    public void run(String... args) {
        User admin = seedAdmin();
        List<Team> teams = seedTeams();
        List<User> citizens = seedCitizens(20);
        List<User> operators = seedOperators(10, teams);
        seedReports(60, citizens, teams, operators);
    }

    private User seedAdmin() {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Admin già presente, seeding saltato");
            return userRepository.findByUsername(adminUsername).orElseThrow();
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("Amministratore CivicFix");
        admin.setRole(Role.ADMIN);

        admin = userRepository.save(admin);
        log.info("Utente Admin creato: {}", adminUsername);
        return admin;
    }

    /** Un team per categoria, così ogni tipo di segnalazione ha una squadra assegnabile. */
    private List<Team> seedTeams() {
        record Modello(String nome, ReportCategory categoria) {}
        Modello[] modelli = {
                new Modello("Squadra Viabilità", ReportCategory.VIABILITY),
                new Modello("Squadra Illuminazione", ReportCategory.LIGHTING),
                new Modello("Squadra Rifiuti", ReportCategory.WASTE),
                new Modello("Squadra Verde Pubblico", ReportCategory.GREEN_AREAS),
                new Modello("Squadra Idrica", ReportCategory.WATER),
                new Modello("Squadra Interventi Generali", ReportCategory.OTHER),
        };

        List<Team> teams = new ArrayList<>();
        for (Modello m : modelli) {
            Team team = teamRepository.findAll().stream()
                    .filter(t -> t.getName().equals(m.nome()))
                    .findFirst()
                    .orElseGet(() -> {
                        Team nuovo = new Team();
                        nuovo.setName(m.nome());
                        nuovo.setCategory(m.categoria());
                        Team salvato = teamRepository.save(nuovo);
                        log.info("Team creato: {}", m.nome());
                        return salvato;
                    });
            teams.add(team);
        }
        return teams;
    }

    private List<User> seedCitizens(int quantita) {
        List<User> citizens = new ArrayList<>();

        // Il cittadino "storico" resta al primo posto per compatibilità con
        // eventuali riferimenti manuali fatti durante lo sviluppo.
        citizens.add(seedUtente("citizen1", "citizen1@test.com", "Cittadino Uno", Role.CITIZEN, null));

        for (int i = 2; i <= quantita; i++) {
            String nomeCompleto = NOMI[random.nextInt(NOMI.length)] + " " + COGNOMI[random.nextInt(COGNOMI.length)];
            citizens.add(seedUtente("citizen" + i, "citizen" + i + "@test.com", nomeCompleto, Role.CITIZEN, null));
        }
        return citizens;
    }

    private List<User> seedOperators(int quantita, List<Team> teams) {
        List<User> operators = new ArrayList<>();
        for (int i = 1; i <= quantita; i++) {
            // Distribuiti a rotazione sui team: ognuno ne ha almeno uno, nessuno resta scoperto.
            Team team = teams.get((i - 1) % teams.size());
            String nomeCompleto = NOMI[random.nextInt(NOMI.length)] + " " + COGNOMI[random.nextInt(COGNOMI.length)];
            operators.add(seedUtente("operatore" + i, "operatore" + i + "@civicfix.local", nomeCompleto, Role.OPERATOR, team));
        }
        return operators;
    }

    private User seedUtente(String username, String email, String nomeCompleto, Role ruolo, Team team) {
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }

        User utente = new User();
        utente.setUsername(username);
        utente.setEmail(email);
        utente.setPasswordHash(passwordEncoder.encode("password123"));
        utente.setFullName(nomeCompleto);
        utente.setRole(ruolo);
        utente.setTeam(team);

        return userRepository.save(utente);
    }

    /**
     * Genera segnalazioni distribuite su tutti gli stati: alcune ancora in
     * attesa, alcune in corso con team e operatore assegnati, altre chiuse
     * (risolte o respinte) con priorità variabile. Serve a poter provare
     * subito filtri, mappa e dashboard senza crearle a mano.
     */
    private void seedReports(int quantita, List<User> citizens, List<Team> teams, List<User> operators) {
        if (reportRepository.count() > 0) {
            log.info("Segnalazioni già presenti, seeding saltato");
            return;
        }

        ReportStatus[] stati = ReportStatus.values();
        ReportPriority[] priorita = ReportPriority.values();

        for (int i = 0; i < quantita; i++) {
            ModelloSegnalazione modello = MODELLI[i % MODELLI.length];
            User autore = citizens.get(random.nextInt(citizens.size()));

            Report report = new Report();
            // Titoli non ripetuti: il modello si ricicla più volte sulle 60 segnalazioni.
            report.setTitle(modello.titolo() + (i >= MODELLI.length ? " (" + (i / MODELLI.length + 1) + ")" : ""));
            report.setDescription(modello.descrizione());
            report.setCategory(modello.categoria());
            report.setLatitude(LAT_BASE + modello.dLat() + jitter());
            report.setLongitude(LNG_BASE + modello.dLng() + jitter());
            report.setAddress(modello.via() + ", Lecce");
            report.setReporter(autore);
            report.setPriority(priorita[random.nextInt(priorita.length)]);

            ReportStatus stato = stati[random.nextInt(stati.length)];
            report.setStatus(stato);

            // Solo chi non è più in attesa ha davvero un team e un operatore:
            // altrimenti la dashboard "Da assegnare" risulterebbe già vuota.
            if (stato != ReportStatus.PENDING) {
                Team team = teams.stream()
                        .filter(t -> t.getCategory() == modello.categoria())
                        .findFirst()
                        .orElse(teams.get(random.nextInt(teams.size())));
                report.setAssignedTeam(team);

                List<User> operatoriDelTeam = operators.stream().filter(o -> team.equals(o.getTeam())).toList();
                if (!operatoriDelTeam.isEmpty()) {
                    report.setAssignedOperator(operatoriDelTeam.get(random.nextInt(operatoriDelTeam.size())));
                }
            }

            reportRepository.save(report);
        }

        log.info("Create {} segnalazioni di prova", quantita);
    }

    /** Piccola variazione casuale (± ~150 m) così i punti non finiscono tutti sovrapposti. */
    private double jitter() {
        return (random.nextDouble() - 0.5) * 0.003;
    }
}
