package com.examensw1.umlcollab.features.project.controller;

import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only API used exclusively by public project links. */
@RestController
@RequestMapping("/api/shared/projects")
@RequiredArgsConstructor
public class SharedProjectController {
    private final ProjectService projectService;
    private final DiagramService diagramService;

    @GetMapping("/{shareToken}")
    public ProjectResponse getProject(@PathVariable UUID shareToken) {
        return projectService.findSharedByToken(shareToken);
    }

    @GetMapping("/{shareToken}/diagrams")
    public List<DiagramResponse> listDiagrams(@PathVariable UUID shareToken) {
        return diagramService.findSharedByToken(shareToken);
    }

    @GetMapping("/{shareToken}/diagrams/{diagramId}")
    public DiagramDetailsResponse getDiagram(@PathVariable UUID shareToken, @PathVariable UUID diagramId) {
        return diagramService.getSharedDetails(shareToken, diagramId);
    }
}
