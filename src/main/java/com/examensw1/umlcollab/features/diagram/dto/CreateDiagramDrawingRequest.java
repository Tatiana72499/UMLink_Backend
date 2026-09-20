package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record CreateDiagramDrawingRequest(
        @NotBlank(message = "El trazo es obligatorio.")
        @Size(max = 12000, message = "El trazo no puede superar 12000 caracteres.") String svgPath,
        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "El color del trazo debe ser hexadecimal.") String strokeColor) {
    public CreateDiagramDrawingRequest(String svgPath) { this(svgPath, "#315B85"); }
}
