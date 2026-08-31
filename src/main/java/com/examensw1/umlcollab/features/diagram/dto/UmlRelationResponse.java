package com.examensw1.umlcollab.features.diagram.dto;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.util.UUID;
public record UmlRelationResponse(UUID id, UUID diagramId, UUID sourceClassId, UUID targetClassId, RelationType type) {}
