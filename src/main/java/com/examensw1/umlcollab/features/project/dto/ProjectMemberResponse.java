package com.examensw1.umlcollab.features.project.dto;

import com.examensw1.umlcollab.features.project.model.ProjectRole;
import java.util.UUID;

public record ProjectMemberResponse(UUID id, UUID userId, String name, String email, ProjectRole role) { }
