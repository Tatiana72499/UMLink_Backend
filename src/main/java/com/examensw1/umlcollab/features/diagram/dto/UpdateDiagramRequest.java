package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDiagramRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(0) Long version) {}
