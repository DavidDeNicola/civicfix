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

    @Override
    public void run(String... args) {
        User admin = seedAdmin();
        Team team = seedTeam();
        User citizen = seedCitizen();
        User operator = seedOperator(team);
        seedReport(citizen, team, operator);
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

    private Team seedTeam() {
        String teamName = "Squadra Viabilità";
        return teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(teamName))
                .findFirst()
                .orElseGet(() -> {
                    Team team = new Team();
                    team.setName(teamName);
                    team.setCategory(ReportCategory.VIABILITY);
                    Team saved = teamRepository.save(team);
                    log.info("Team di test creato: {}", teamName);
                    return saved;
                });
    }

    private User seedCitizen() {
        String username = "citizen1";
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }

        User citizen = new User();
        citizen.setUsername(username);
        citizen.setEmail("citizen1@test.com");
        citizen.setPasswordHash(passwordEncoder.encode("password123"));
        citizen.setFullName("Cittadino Uno");
        citizen.setRole(Role.CITIZEN);

        User saved = userRepository.save(citizen);
        log.info("Cittadino di test creato: {}", username);
        return saved;
    }

    private User seedOperator(Team team) {
        String username = "operatore1";
        if (userRepository.existsByUsername(username)) {
            return userRepository.findByUsername(username).orElseThrow();
        }

        User operator = new User();
        operator.setUsername(username);
        operator.setEmail("operatore1@civicfix.local");
        operator.setPasswordHash(passwordEncoder.encode("password123"));
        operator.setFullName("Mario Rossi");
        operator.setRole(Role.OPERATOR);
        operator.setTeam(team);

        User saved = userRepository.save(operator);
        log.info("Operatore di test creato: {} (team: {})", username, team.getName());
        return saved;
    }

    private void seedReport(User citizen, Team team, User operator) {
        if (reportRepository.count() > 0) {
            return;
        }

        Report report = new Report();
        report.setTitle("Buca pericolosa");
        report.setDescription("Buca profonda vicino al marciapiede");
        report.setCategory(ReportCategory.VIABILITY);
        report.setLatitude(40.3515);
        report.setLongitude(18.1750);
        report.setAddress("Via Roma, Lecce");
        report.setReporter(citizen);
        report.setAssignedTeam(team);
        report.setAssignedOperator(operator);
        report.setStatus(ReportStatus.IN_PROGRESS);

        reportRepository.save(report);
        log.info("Segnalazione di test creata: {}", report.getTitle());
    }
}