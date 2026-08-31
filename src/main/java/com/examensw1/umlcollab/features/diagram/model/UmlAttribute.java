package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "uml_attributes") @Getter @Setter @NoArgsConstructor
public class UmlAttribute {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "uml_class_id", nullable = false) private UUID umlClassId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "data_type", nullable = false, length = 80) private String dataType;
    @Column(nullable = false, length = 20) private String visibility = "PRIVATE";
}
