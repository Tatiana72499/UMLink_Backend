package com.examensw1.umlcollab.features.generation.controller;

import com.examensw1.umlcollab.features.generation.service.BackendGenerationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagrams/{diagramId}/generate")
@RequiredArgsConstructor
public class BackendGenerationController {
    private final BackendGenerationService service;

    @GetMapping("/backend")
    public ResponseEntity<byte[]> generate(@PathVariable UUID diagramId) {
        BackendGenerationService.GeneratedBackend artifact = service.generate(diagramId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + artifact.fileName())
                .body(artifact.content());
    }
}
