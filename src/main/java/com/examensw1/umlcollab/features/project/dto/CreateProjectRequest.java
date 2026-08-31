package com.examensw1.umlcollab.features.project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank String name,
        String description,
        @NotBlank String ownerName) {}
