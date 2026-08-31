package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.features.diagram.dto.*;
import com.examensw1.umlcollab.features.diagram.model.*;
import com.examensw1.umlcollab.features.diagram.repository.*;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Slf4j @RequiredArgsConstructor
public class DiagramService {
    private final ProjectService projectService;
    private final DiagramRepository diagrams;
    private final UmlClassRepository classes;
    private final UmlAttributeRepository attributes;
    private final UmlRelationRepository relations;

    @Transactional public DiagramResponse createDiagram(UUID projectId, CreateDiagramRequest request) {
        projectService.findEntity(projectId);
        Diagram diagram = new Diagram(); diagram.setProjectId(projectId); diagram.setName(request.name());
        Diagram saved = diagrams.save(diagram); log.info("Diagrama creado: {}", saved.getId()); return toResponse(saved);
    }
    public List<DiagramResponse> findByProject(UUID projectId) { projectService.findEntity(projectId); return diagrams.findByProjectId(projectId).stream().map(this::toResponse).toList(); }
    public DiagramDetailsResponse getDetails(UUID diagramId) {
        Diagram diagram = findDiagram(diagramId);
        return new DiagramDetailsResponse(toResponse(diagram), classes.findByDiagramId(diagramId).stream().map(this::toResponse).toList(), relations.findByDiagramId(diagramId).stream().map(this::toResponse).toList());
    }
    @Transactional public UmlClassResponse createClass(UUID diagramId, CreateUmlClassRequest request) {
        findDiagram(diagramId); UmlClass umlClass = new UmlClass(); umlClass.setDiagramId(diagramId); umlClass.setName(request.name()); umlClass.setPositionX(request.positionX()); umlClass.setPositionY(request.positionY());
        return toResponse(classes.save(umlClass));
    }
    @Transactional public UmlClassResponse updateClass(UUID id, UpdateUmlClassRequest request) {
        UmlClass umlClass = findClass(id); umlClass.setName(request.name()); umlClass.setPositionX(request.positionX()); umlClass.setPositionY(request.positionY()); return toResponse(classes.save(umlClass));
    }
    @Transactional public void deleteClass(UUID id) { classes.delete(findClass(id)); log.info("Clase UML eliminada: {}", id); }
    @Transactional public UmlAttributeResponse createAttribute(UUID classId, CreateAttributeRequest request) {
        findClass(classId); UmlAttribute attribute = new UmlAttribute(); attribute.setUmlClassId(classId); attribute.setName(request.name()); attribute.setDataType(request.dataType()); attribute.setVisibility(request.visibility() == null ? "PRIVATE" : request.visibility()); return toResponse(attributes.save(attribute));
    }
    @Transactional public void deleteAttribute(UUID id) { attributes.delete(attributes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Atributo UML", id))); }
    @Transactional public UmlRelationResponse createRelation(UUID diagramId, CreateRelationRequest request) {
        findDiagram(diagramId); UmlClass source = findClass(request.sourceClassId()); UmlClass target = findClass(request.targetClassId());
        if (!source.getDiagramId().equals(diagramId) || !target.getDiagramId().equals(diagramId)) throw new IllegalArgumentException("Las clases deben pertenecer al mismo diagrama.");
        UmlRelation relation = new UmlRelation(); relation.setDiagramId(diagramId); relation.setSourceClassId(request.sourceClassId()); relation.setTargetClassId(request.targetClassId()); relation.setType(request.type()); return toResponse(relations.save(relation));
    }
    @Transactional public void deleteRelation(UUID id) { relations.delete(relations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Relación UML", id))); }
    private Diagram findDiagram(UUID id) { return diagrams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Diagrama", id)); }
    private UmlClass findClass(UUID id) { return classes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clase UML", id)); }
    private DiagramResponse toResponse(Diagram item) { return new DiagramResponse(item.getId(), item.getProjectId(), item.getName(), item.getVersion(), item.getCreatedAt()); }
    private UmlClassResponse toResponse(UmlClass item) { return new UmlClassResponse(item.getId(), item.getDiagramId(), item.getName(), item.getPositionX(), item.getPositionY(), item.getVersion()); }
    private UmlAttributeResponse toResponse(UmlAttribute item) { return new UmlAttributeResponse(item.getId(), item.getUmlClassId(), item.getName(), item.getDataType(), item.getVisibility()); }
    private UmlRelationResponse toResponse(UmlRelation item) { return new UmlRelationResponse(item.getId(), item.getDiagramId(), item.getSourceClassId(), item.getTargetClassId(), item.getType()); }
}
