package com.examensw1.umlcollab.features.collaboration.controller;

import com.examensw1.umlcollab.features.collaboration.dto.DiagramActivityResponse;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagrams")
@RequiredArgsConstructor
public class DiagramActivityController {

    private final CollaborationService collaborationService;

    @GetMapping("/{diagramId}/activity")
    public List<DiagramActivityResponse> list(@PathVariable UUID diagramId) {
        return collaborationService.findActivity(diagramId);
    }
}
