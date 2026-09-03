package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "uml_classes") @Getter @Setter @NoArgsConstructor
public class UmlClass {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "diagram_id", nullable = false) private UUID diagramId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "position_x", nullable = false) private double positionX;
    @Column(name = "position_y", nullable = false) private double positionY;
    @Column(name = "fill_color", length = 7) private String fillColor;
    @Version private Long version;
}
