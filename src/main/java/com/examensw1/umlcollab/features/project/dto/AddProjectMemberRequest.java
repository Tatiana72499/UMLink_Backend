package com.examensw1.umlcollab.features.project.dto;

import com.examensw1.umlcollab.features.project.model.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String email,
        @NotNull ProjectRole role) { }
