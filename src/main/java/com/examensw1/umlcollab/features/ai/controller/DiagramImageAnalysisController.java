package com.examensw1.umlcollab.features.ai.controller;

import com.examensw1.umlcollab.features.ai.dto.DiagramImagePreviewResponse;
import com.examensw1.umlcollab.features.ai.service.DiagramImageAnalysisService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects/{projectId}/diagrams/ai")
@RequiredArgsConstructor
public class DiagramImageAnalysisController {
    private final DiagramImageAnalysisService service;

    @PostMapping(value = "/image-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DiagramImagePreviewResponse preview(@PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        try {
            return service.preview(projectId, file.getBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new IllegalArgumentException("No pudimos leer la imagen seleccionada.");
        }
    }
}
