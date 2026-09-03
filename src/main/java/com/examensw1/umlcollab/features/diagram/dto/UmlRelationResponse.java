package com.examensw1.umlcollab.features.diagram.dto;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.util.UUID;
import java.util.List;
public record UmlRelationResponse(UUID id, UUID diagramId, UUID sourceClassId, UUID targetClassId, RelationType type, String label, String sourceCardinality, String targetCardinality, Double bendX, Double bendY, UUID associationClassId, List<RelationAlignmentPoint> alignmentPoints) {
    public UmlRelationResponse(UUID id, UUID diagramId, UUID sourceClassId, UUID targetClassId, RelationType type, String label, String sourceCardinality, String targetCardinality) {
        this(id, diagramId, sourceClassId, targetClassId, type, label, sourceCardinality, targetCardinality, null, null, null, List.of());
    }
}
