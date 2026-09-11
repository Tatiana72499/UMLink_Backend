package com.examensw1.umlcollab.features.ai.service;

import com.examensw1.umlcollab.features.ai.dto.DiagramImagePreviewResponse;
import com.examensw1.umlcollab.features.diagram.service.UmlInterchangeService;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiagramImageAnalysisService {
    private static final int MAX_IMAGE_SIZE = 2_000_000;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final ProjectService projectService;
    private final DiagramImageAiClient imageAiClient;
    private final UmlInterchangeService interchangeService;

    @Transactional(readOnly = true)
    public DiagramImagePreviewResponse preview(UUID projectId, byte[] image, String contentType) {
        projectService.findEditableEntity(projectId);
        validateImage(image, contentType);
        String plantUml = imageAiClient.generatePlantUml(image, contentType.toLowerCase(Locale.ROOT));
        UmlInterchangeService.ImportedDiagram parsed = interchangeService.parse(
                plantUml.getBytes(StandardCharsets.UTF_8), "propuesta-ia.puml");
        log.info("Vista previa IA generada para proyecto {}: {} clases, {} relaciones", projectId, parsed.classes().size(), parsed.relations().size());
        return new DiagramImagePreviewResponse(plantUml, "Diagrama desde imagen", parsed.classes().size(), parsed.relations().size());
    }

    private void validateImage(byte[] image, String contentType) {
        if (image == null || image.length == 0) throw new IllegalArgumentException("Selecciona una imagen PNG, JPG o WEBP con contenido.");
        if (image.length > MAX_IMAGE_SIZE) throw new IllegalArgumentException("La imagen supera el límite de 2 MB permitido para el análisis por IA.");
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("El archivo seleccionado no es una imagen PNG, JPG o WEBP compatible.");
        }
    }
}
