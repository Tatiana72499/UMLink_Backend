package com.examensw1.umlcollab.features.diagram.controller;

import com.examensw1.umlcollab.features.diagram.dto.*;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class DiagramController {
    private final DiagramService service;
    @PostMapping("/projects/{projectId}/diagrams") @ResponseStatus(HttpStatus.CREATED) public DiagramResponse createDiagram(@PathVariable UUID projectId, @Valid @RequestBody CreateDiagramRequest request) { return service.createDiagram(projectId, request); }
    @GetMapping("/projects/{projectId}/diagrams") public List<DiagramResponse> listDiagrams(@PathVariable UUID projectId) { return service.findByProject(projectId); }
    @GetMapping("/diagrams/{diagramId}") public DiagramDetailsResponse getDiagram(@PathVariable UUID diagramId) { return service.getDetails(diagramId); }
    @PostMapping("/diagrams/{diagramId}/classes") @ResponseStatus(HttpStatus.CREATED) public UmlClassResponse createClass(@PathVariable UUID diagramId, @Valid @RequestBody CreateUmlClassRequest request) { return service.createClass(diagramId, request); }
    @PutMapping("/classes/{id}") public UmlClassResponse updateClass(@PathVariable UUID id, @Valid @RequestBody UpdateUmlClassRequest request) { return service.updateClass(id, request); }
    @DeleteMapping("/classes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteClass(@PathVariable UUID id) { service.deleteClass(id); }
    @PostMapping("/classes/{classId}/attributes") @ResponseStatus(HttpStatus.CREATED) public UmlAttributeResponse createAttribute(@PathVariable UUID classId, @Valid @RequestBody CreateAttributeRequest request) { return service.createAttribute(classId, request); }
    @DeleteMapping("/attributes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAttribute(@PathVariable UUID id) { service.deleteAttribute(id); }
    @PostMapping("/diagrams/{diagramId}/relations") @ResponseStatus(HttpStatus.CREATED) public UmlRelationResponse createRelation(@PathVariable UUID diagramId, @Valid @RequestBody CreateRelationRequest request) { return service.createRelation(diagramId, request); }
    @DeleteMapping("/relations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteRelation(@PathVariable UUID id) { service.deleteRelation(id); }
}
