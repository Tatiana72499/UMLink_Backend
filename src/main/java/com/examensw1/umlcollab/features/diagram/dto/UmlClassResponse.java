package com.examensw1.umlcollab.features.diagram.dto;
import java.util.UUID;
public record UmlClassResponse(UUID id, UUID diagramId, String name, double positionX, double positionY, Long version) {}
