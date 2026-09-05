package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "diagram_drawings")
@Getter
@Setter
@NoArgsConstructor
public class DiagramDrawing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "diagram_id", nullable = false)
    private UUID diagramId;

    @Column(name = "svg_path", nullable = false, columnDefinition = "TEXT")
    private String svgPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void created() {
        createdAt = Instant.now();
    }
}
