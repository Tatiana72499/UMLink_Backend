package com.examensw1.umlcollab.features.diagram.controller;

import com.examensw1.umlcollab.features.diagram.dto.*;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import com.examensw1.umlcollab.features.diagram.service.DiagramAssistantService;
import com.examensw1.umlcollab.features.diagram.model.InterchangeFormat;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@RestController @Validated @RequestMapping("/api") @RequiredArgsConstructor
public class DiagramController {
    private final DiagramService service;
    private final DiagramAssistantService assistantService;
    @PostMapping("/projects/{projectId}/diagrams") @ResponseStatus(HttpStatus.CREATED) public DiagramResponse createDiagram(@PathVariable UUID projectId, @Valid @RequestBody CreateDiagramRequest request) { return service.createDiagram(projectId, request); }
    @GetMapping("/projects/{projectId}/diagrams") public List<DiagramResponse> listDiagrams(@PathVariable UUID projectId) { return service.findByProject(projectId); }
    @GetMapping("/diagrams/{diagramId}") public DiagramDetailsResponse getDiagram(@PathVariable UUID diagramId) { return service.getDetails(diagramId); }
    @PostMapping("/diagrams/{diagramId}/assistant/commands") public AssistantCommandResponse executeAssistant(@PathVariable UUID diagramId, @Valid @RequestBody ExecuteAssistantCommandRequest request) { return assistantService.execute(diagramId, request); }
    @GetMapping("/diagrams/{diagramId}/export") public ResponseEntity<byte[]> exportDiagram(@PathVariable UUID diagramId, @RequestParam InterchangeFormat format) {
        String extension = format == InterchangeFormat.XML ? "xml" : format == InterchangeFormat.EA_SCRIPT ? "js" : format == InterchangeFormat.PLANT_UML ? "puml" : "xmi";
        MediaType contentType = format == InterchangeFormat.EA_SCRIPT ? MediaType.parseMediaType("application/javascript") : format == InterchangeFormat.PLANT_UML ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_XML;
        return ResponseEntity.ok().contentType(contentType).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=diagram." + extension).body(service.exportDiagram(diagramId, format));
    }
    @PostMapping(value = "/projects/{projectId}/diagrams/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public DiagramResponse importDiagram(@PathVariable UUID projectId, @RequestParam("file") MultipartFile file) {
        try { return service.importDiagram(projectId, file.getBytes(), file.getOriginalFilename()); }
        catch (java.io.IOException ex) { throw new IllegalArgumentException("No pudimos leer el archivo seleccionado."); }
    }
    @PutMapping("/diagrams/{diagramId}") public DiagramResponse updateDiagram(@PathVariable UUID diagramId, @Valid @RequestBody UpdateDiagramRequest request) { return service.updateDiagram(diagramId, request); }
    @DeleteMapping("/diagrams/{diagramId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteDiagram(@PathVariable UUID diagramId, @RequestParam @jakarta.validation.constraints.Min(0) Long version) { service.deleteDiagram(diagramId, version); }
    @PostMapping("/diagrams/{diagramId}/drawings") @ResponseStatus(HttpStatus.CREATED) public DiagramDrawingResponse createDrawing(@PathVariable UUID diagramId, @Valid @RequestBody CreateDiagramDrawingRequest request) { return service.createDrawing(diagramId, request); }
    @DeleteMapping("/diagrams/{diagramId}/drawings/{drawingId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteDrawing(@PathVariable UUID diagramId, @PathVariable UUID drawingId) { service.deleteDrawing(diagramId, drawingId); }
    @DeleteMapping("/diagrams/{diagramId}/drawings") @ResponseStatus(HttpStatus.NO_CONTENT) public void clearDrawings(@PathVariable UUID diagramId) { service.clearDrawings(diagramId); }
    @PostMapping("/diagrams/{diagramId}/classes") @ResponseStatus(HttpStatus.CREATED) public UmlClassResponse createClass(@PathVariable UUID diagramId, @Valid @RequestBody CreateUmlClassRequest request) { return service.createClass(diagramId, request); }
    @PutMapping("/classes/{id}") public UmlClassResponse updateClass(@PathVariable UUID id, @Valid @RequestBody UpdateUmlClassRequest request) { return service.updateClass(id, request); }
    @DeleteMapping("/classes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteClass(@PathVariable UUID id) { service.deleteClass(id); }
    @PostMapping("/classes/{classId}/attributes") @ResponseStatus(HttpStatus.CREATED) public UmlAttributeResponse createAttribute(@PathVariable UUID classId, @Valid @RequestBody CreateAttributeRequest request) { return service.createAttribute(classId, request); }
    @PutMapping("/attributes/{id}") public UmlAttributeResponse updateAttribute(@PathVariable UUID id, @Valid @RequestBody UpdateAttributeRequest request) { return service.updateAttribute(id, request); }
    @PutMapping("/classes/{classId}/attributes/order") public List<UmlAttributeResponse> reorderAttributes(@PathVariable UUID classId, @Valid @RequestBody UpdateAttributeOrderRequest request) { return service.reorderAttributes(classId, request); }
    @DeleteMapping("/attributes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAttribute(@PathVariable UUID id) { service.deleteAttribute(id); }
    @PostMapping("/classes/{classId}/operations") @ResponseStatus(HttpStatus.CREATED) public UmlOperationResponse createOperation(@PathVariable UUID classId, @Valid @RequestBody CreateUmlOperationRequest request) { return service.createOperation(classId, request); }
    @PutMapping("/operations/{id}") public UmlOperationResponse updateOperation(@PathVariable UUID id, @Valid @RequestBody UpdateUmlOperationRequest request) { return service.updateOperation(id, request); }
    @DeleteMapping("/operations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOperation(@PathVariable UUID id) { service.deleteOperation(id); }
    @PostMapping("/diagrams/{diagramId}/relations") @ResponseStatus(HttpStatus.CREATED) public UmlRelationResponse createRelation(@PathVariable UUID diagramId, @Valid @RequestBody CreateRelationRequest request) { return service.createRelation(diagramId, request); }
    @PostMapping("/diagrams/{diagramId}/association-classes") @ResponseStatus(HttpStatus.CREATED) public AssociationClassResponse createAssociationClass(@PathVariable UUID diagramId, @Valid @RequestBody CreateAssociationClassRequest request) { return service.createAssociationClass(diagramId, request); }
    @PutMapping("/relations/{id}") public UmlRelationResponse updateRelation(@PathVariable UUID id, @Valid @RequestBody UpdateRelationRequest request) { return service.updateRelation(id, request); }
    @PutMapping("/relations/{id}/cardinality") public UmlRelationResponse updateRelationCardinality(@PathVariable UUID id, @Valid @RequestBody UpdateRelationCardinalityRequest request) { return service.updateRelationCardinality(id, request); }
    @DeleteMapping("/relations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteRelation(@PathVariable UUID id) { service.deleteRelation(id); }
}
