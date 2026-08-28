package org.civicfix.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_photos")
@Getter
@Setter
@NoArgsConstructor

/**
 * Foto allegata a una segnalazione. Il file sta sul filesystem del server,
 * qui si salva solo il percorso (filePath).
 */

public class ReportPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(nullable = false)
    private String filePath;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}
