package com.examensw1.umlcollab.features.project.dto;

import com.examensw1.umlcollab.features.project.model.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberRequest(@NotNull ProjectRole role) { }
