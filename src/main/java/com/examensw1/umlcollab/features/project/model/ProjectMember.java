package com.examensw1.umlcollab.features.project.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "project_members", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ProjectRole role;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void created() { createdAt = Instant.now(); }
}
