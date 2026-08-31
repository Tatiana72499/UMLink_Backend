package com.examensw1.umlcollab.features.platos.dto;
import java.time.Instant;
import java.util.UUID;
public record DiagramResponse(UUID id, UUID projectId, String name, Long version, Instant createdAt) {}
