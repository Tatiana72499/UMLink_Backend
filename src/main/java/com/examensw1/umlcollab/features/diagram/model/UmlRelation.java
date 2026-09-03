package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "uml_relations") @Getter @Setter @NoArgsConstructor
public class UmlRelation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "diagram_id", nullable = false) private UUID diagramId;
    @Column(name = "source_class_id", nullable = false) private UUID sourceClassId;
    @Column(name = "target_class_id", nullable = false) private UUID targetClassId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private RelationType type;
    @Column(length = 120) private String label;
    @Column(name = "source_cardinality", length = 20) private String sourceCardinality;
    @Column(name = "target_cardinality", length = 20) private String targetCardinality;
    @Column(name = "bend_x") private Double bendX;
    @Column(name = "bend_y") private Double bendY;
    @Column(name = "association_class_id") private UUID associationClassId;
    @Column(name = "alignment_points") private String alignmentPoints;
}
