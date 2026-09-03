package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAssociationClassRequest(
        @NotNull UUID sourceClassId,
        @NotNull UUID targetClassId,
        @NotBlank @Size(max = 120) String name,
        @DecimalMin("0.0") double positionX,
        @DecimalMin("0.0") double positionY,
        @jakarta.validation.constraints.Pattern(regexp = "#[0-9A-Fa-f]{6}") String fillColor,
        @Size(max = 120) String label) {}
