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
}
