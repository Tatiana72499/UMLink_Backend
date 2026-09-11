package com.examensw1.umlcollab.features.ai.dto;

/** Vista previa no persistida que debe ser confirmada por la persona usuaria. */
public record DiagramImagePreviewResponse(
        String plantUml,
        String suggestedName,
        int classCount,
        int relationCount) {
}
