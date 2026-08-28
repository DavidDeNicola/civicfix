package org.civicfix.app.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor

/**
 * Squadra di operatori specializzata in una categoria di segnalazioni.
 * Le segnalazioni si assegnano prima al team, poi a un singolo operatore del team.
 */

public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportCategory category;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "team")
    private List<User> members = new ArrayList<>();
}
