package com.examensw1.umlcollab.features.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(UUID id, String name, String description, String ownerName, Long version, Instant createdAt) {}
