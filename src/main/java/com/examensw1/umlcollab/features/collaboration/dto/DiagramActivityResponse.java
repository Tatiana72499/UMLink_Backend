package com.examensw1.umlcollab.features.collaboration.dto;

import java.time.Instant;
import java.util.UUID;

public record DiagramActivityResponse(UUID id, UUID actorId, String actorName, String action, Instant createdAt) {
}
