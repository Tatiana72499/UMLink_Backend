package com.examensw1.umlcollab.features.collaboration.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DiagramEvent(@NotNull UUID diagramId, @NotNull DiagramEventType type, JsonNode payload,
        CollaborationParticipant actor) {
}
