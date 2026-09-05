package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiagramDrawingRequest(
        @NotBlank(message = "El trazo es obligatorio.")
        @Size(max = 12000, message = "El trazo no puede superar 12000 caracteres.") String svgPath) {
}
