package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "diagrams") @Getter @Setter @NoArgsConstructor
public class Diagram {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false, length = 120) private String name;
    @Version private Long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void created() { createdAt = Instant.now(); }
}
