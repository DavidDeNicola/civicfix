package org.civicfix.app.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor

/**
 * Utente della piattaforma: cittadino, operatore comunale o admin.
 * Il campo role determina i permessi; se operatore, è collegato a un Team.
 */

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Recupero password: si salva l'hash SHA-256 del token, mai il token in
    // chiaro, così chi legge il database non può usarlo per impersonare
    // l'utente. Entrambi i campi vengono azzerati appena il token è usato.
    @Column(unique = true)
    private String resetTokenHash;

    private LocalDateTime resetTokenExpiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
