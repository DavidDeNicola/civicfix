package org.civicfix.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="updates")
@Getter
@Setter
@NoArgsConstructor

/**
 * Voce della cronologia di una segnalazione: un commento oppure un cambio di stato.
 * Se è un cambio di stato, oldStatus/newStatus tracciano il prima e il dopo.
 */

public class Update {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UpdateType type;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private ReportStatus oldStatus;

    @Enumerated(EnumType.STRING)
    private ReportStatus newStatus;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
