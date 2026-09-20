package com.examensw1.umlcollab.features.diagram.dto;

import java.util.UUID;

public record DiagramDrawingResponse(UUID id, String svgPath, String strokeColor) {
    public DiagramDrawingResponse(UUID id, String svgPath) { this(id, svgPath, "#315B85"); }
}
