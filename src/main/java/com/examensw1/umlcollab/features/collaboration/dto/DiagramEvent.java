package com.examensw1.umlcollab.features.collaboration.dto;
import java.util.UUID;
public record DiagramEvent(UUID diagramId, String type, Object payload) {}
