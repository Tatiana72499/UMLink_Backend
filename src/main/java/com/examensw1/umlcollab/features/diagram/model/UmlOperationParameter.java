package com.examensw1.umlcollab.features.diagram.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "uml_operation_parameters")
@Getter @Setter @NoArgsConstructor
public class UmlOperationParameter {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "uml_operation_id", nullable = false) private UUID umlOperationId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "data_type", nullable = false, length = 80) private String dataType;
    @Column(name = "parameter_order", nullable = false) private int parameterOrder;
}
