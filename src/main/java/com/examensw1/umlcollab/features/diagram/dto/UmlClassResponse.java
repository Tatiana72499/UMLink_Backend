package com.examensw1.umlcollab.features.diagram.dto;
import java.util.List;
import java.util.UUID;
public record UmlClassResponse(UUID id, UUID diagramId, String name, double positionX, double positionY, String fillColor, Long version, List<UmlAttributeResponse> attributes) {}
