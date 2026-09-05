package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final UmlOperationRepository operations;
    private final UmlOperationParameterRepository operationParameters;
    private final UmlRelationRepository relations;
    private final DiagramDrawingRepository drawings;
    private final ObjectMapper objectMapper;
    private final CollaborationService collaborationService;

    @Transactional public DiagramResponse createDiagram(UUID projectId, CreateDiagramRequest request) {
        projectService.findEditableEntity(projectId);
        Diagram diagram = new Diagram(); diagram.setProjectId(projectId); diagram.setName(request.name());
        Diagram saved = diagrams.save(diagram); log.info("Diagrama creado: {}", saved.getId()); return toResponse(saved);
    }
    public List<DiagramResponse> findByProject(UUID projectId) { projectService.findEntity(projectId); return diagrams.findByProjectId(projectId).stream().map(this::toResponse).toList(); }
    @Transactional public DiagramResponse updateDiagram(UUID diagramId, UpdateDiagramRequest request) {
        Diagram diagram = findDiagram(diagramId);
        verifyExpectedVersion("El diagrama", diagram.getVersion(), request.version());
        diagram.setName(request.name());
        Diagram saved = diagrams.saveAndFlush(diagram);
        collaborationService.publishDiagramChanged(saved.getId(), "actualizó el nombre del diagrama");
        log.info("Diagrama actualizado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public void deleteDiagram(UUID diagramId, Long version) {
        Diagram diagram = findDiagram(diagramId);
        verifyExpectedVersion("El diagrama", diagram.getVersion(), version);
        diagrams.delete(diagram);
        log.info("Diagrama eliminado: {}", diagramId);
    }
    public DiagramDetailsResponse getDetails(UUID diagramId) {
        Diagram diagram = diagrams.findById(diagramId).orElseThrow(() -> new ResourceNotFoundException("Diagrama", diagramId));
        projectService.findEntity(diagram.getProjectId());
        return new DiagramDetailsResponse(toResponse(diagram), classes.findByDiagramId(diagramId).stream().map(this::toResponse).toList(), relations.findByDiagramId(diagramId).stream().map(this::toResponse).toList(), drawings.findByDiagramIdOrderByCreatedAtAsc(diagramId).stream().map(this::toResponse).toList());
    }
    @Transactional public DiagramDrawingResponse createDrawing(UUID diagramId, CreateDiagramDrawingRequest request) {
        findDiagram(diagramId);
        DiagramDrawing drawing = new DiagramDrawing();
        drawing.setDiagramId(diagramId);
        drawing.setSvgPath(request.svgPath());
        DiagramDrawing saved = drawings.save(drawing);
        collaborationService.publishDiagramChanged(diagramId, "agregó un trazo");
        return toResponse(saved);
    }
    @Transactional public void deleteDrawing(UUID diagramId, UUID drawingId) {
        findDiagram(diagramId);
        DiagramDrawing drawing = drawings.findById(drawingId)
                .filter(item -> item.getDiagramId().equals(diagramId))
                .orElseThrow(() -> new ResourceNotFoundException("Trazo", drawingId));
        drawings.delete(drawing);
        collaborationService.publishDiagramChanged(diagramId, "eliminó un trazo");
    }
    @Transactional public void clearDrawings(UUID diagramId) {
        findDiagram(diagramId);
        drawings.deleteByDiagramId(diagramId);
        collaborationService.publishDiagramChanged(diagramId, "limpió los trazos");
    }
    @Transactional public UmlClassResponse createClass(UUID diagramId, CreateUmlClassRequest request) {
        findDiagram(diagramId); UmlClass umlClass = new UmlClass(); umlClass.setDiagramId(diagramId); umlClass.setName(request.name()); umlClass.setPositionX(request.positionX()); umlClass.setPositionY(request.positionY()); umlClass.setFillColor(request.fillColor());
        UmlClass saved = classes.save(umlClass);
        collaborationService.publishDiagramChanged(diagramId, "creó una clase");
        return toResponse(saved);
    }
    @Transactional public UmlClassResponse updateClass(UUID id, UpdateUmlClassRequest request) {
        UmlClass umlClass = findClass(id); umlClass.setName(request.name()); umlClass.setPositionX(request.positionX()); umlClass.setPositionY(request.positionY()); umlClass.setFillColor(request.fillColor());
        UmlClass saved = classes.save(umlClass);
        collaborationService.publishDiagramChanged(saved.getDiagramId(), "actualizó una clase");
        return toResponse(saved);
    }
    @Transactional public void deleteClass(UUID id) {
        UmlClass umlClass = findClass(id);
        classes.delete(umlClass);
        collaborationService.publishDiagramChanged(umlClass.getDiagramId(), "eliminó una clase");
        log.info("Clase UML eliminada: {}", id);
    }
    @Transactional public UmlAttributeResponse createAttribute(UUID classId, CreateAttributeRequest request) {
        UmlClass umlClass = findClass(classId); UmlAttribute attribute = new UmlAttribute(); attribute.setUmlClassId(classId); attribute.setName(request.name()); attribute.setDataType(request.dataType().displayName()); attribute.setVisibility(request.visibility() == null ? "PRIVATE" : request.visibility());
        UmlAttribute saved = attributes.save(attribute);
        collaborationService.publishDiagramChanged(umlClass.getDiagramId(), "agregó un atributo");
        return toResponse(saved);
    }
    @Transactional public UmlAttributeResponse updateAttribute(UUID id, UpdateAttributeRequest request) {
        UmlAttribute attribute = findAttribute(id);
        attribute.setName(request.name());
        attribute.setDataType(request.dataType().displayName());
        attribute.setVisibility(request.visibility());
        UmlAttribute saved = attributes.save(attribute);
        collaborationService.publishDiagramChanged(findClass(saved.getUmlClassId()).getDiagramId(), "actualizó un atributo");
        log.info("Atributo UML actualizado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public void deleteAttribute(UUID id) {
        UmlAttribute attribute = findAttribute(id);
        attributes.delete(attribute);
        collaborationService.publishDiagramChanged(findClass(attribute.getUmlClassId()).getDiagramId(), "eliminó un atributo");
    }
    @Transactional public UmlOperationResponse createOperation(UUID classId, CreateUmlOperationRequest request) {
        UmlClass umlClass = findClass(classId);
        validateParameterNames(request.parameters());
        UmlOperation operation = new UmlOperation();
        operation.setUmlClassId(classId);
        applyOperationValues(operation, request.name(), request.visibility(), request.returnType());
        UmlOperation saved = operations.save(operation);
        saveOperationParameters(saved.getId(), request.parameters());
        collaborationService.publishDiagramChanged(umlClass.getDiagramId(), "agregó una operación");
        log.info("Operación UML creada: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public UmlOperationResponse updateOperation(UUID id, UpdateUmlOperationRequest request) {
        UmlOperation operation = findOperation(id);
        validateParameterNames(request.parameters());
        applyOperationValues(operation, request.name(), request.visibility(), request.returnType());
        UmlOperation saved = operations.save(operation);
        operationParameters.deleteByUmlOperationId(saved.getId());
        saveOperationParameters(saved.getId(), request.parameters());
        collaborationService.publishDiagramChanged(findClass(saved.getUmlClassId()).getDiagramId(), "actualizó una operación");
        log.info("Operación UML actualizada: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public void deleteOperation(UUID id) {
        UmlOperation operation = findOperation(id);
        operationParameters.deleteByUmlOperationId(id);
        operations.delete(operation);
        collaborationService.publishDiagramChanged(findClass(operation.getUmlClassId()).getDiagramId(), "eliminó una operación");
        log.info("Operación UML eliminada: {}", id);
    }
    @Transactional public UmlRelationResponse createRelation(UUID diagramId, CreateRelationRequest request) {
        findDiagram(diagramId);
        UmlRelation relation = new UmlRelation();
        relation.setDiagramId(diagramId);
        applyRelationValues(relation, request.sourceClassId(), request.targetClassId(), request.type(), request.label(), request.sourceCardinality(), request.targetCardinality(), request.bendX(), request.bendY(), request.associationClassId(), request.alignmentPoints());
        UmlRelation saved = relations.save(relation);
        collaborationService.publishDiagramChanged(diagramId, "creó una relación");
        return toResponse(saved);
    }
    @Transactional public AssociationClassResponse createAssociationClass(UUID diagramId, CreateAssociationClassRequest request) {
        findDiagram(diagramId);
        if (request.sourceClassId().equals(request.targetClassId())) {
            throw new IllegalArgumentException("La clase intermedia requiere dos clases diferentes.");
        }
        UmlClass associationClass = new UmlClass();
        associationClass.setDiagramId(diagramId);
        associationClass.setName(request.name());
        associationClass.setPositionX(request.positionX());
        associationClass.setPositionY(request.positionY());
        associationClass.setFillColor(request.fillColor());
        UmlClass savedAssociationClass = classes.save(associationClass);

        UmlRelation relation = new UmlRelation();
        relation.setDiagramId(diagramId);
        applyRelationValues(relation, request.sourceClassId(), request.targetClassId(), RelationType.ASSOCIATION,
                request.label(), null, null, null, null, savedAssociationClass.getId(), List.of());
        UmlRelation savedRelation = relations.save(relation);
        collaborationService.publishDiagramChanged(diagramId, "creó una clase intermedia");
        log.info("Clase intermedia UML creada: {} para relación {}", savedAssociationClass.getId(), savedRelation.getId());
        return new AssociationClassResponse(toResponse(savedAssociationClass), toResponse(savedRelation));
    }
    @Transactional public UmlRelationResponse updateRelation(UUID id, UpdateRelationRequest request) {
        UmlRelation relation = findRelation(id);
        applyRelationValues(relation, request.sourceClassId(), request.targetClassId(), request.type(), request.label(), request.sourceCardinality(), request.targetCardinality(), request.bendX(), request.bendY(), request.associationClassId(), request.alignmentPoints());
        UmlRelation saved = relations.save(relation);
        collaborationService.publishDiagramChanged(saved.getDiagramId(), "actualizó una relación");
        log.info("Relación UML actualizada: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public UmlRelationResponse updateRelationCardinality(UUID id, UpdateRelationCardinalityRequest request) {
        UmlRelation relation = findRelation(id);
        if (!supportsCardinality(relation.getType())) {
            throw new IllegalArgumentException("Este tipo de relación UML no usa cardinalidad.");
        }
        relation.setSourceCardinality(request.sourceCardinality());
        relation.setTargetCardinality(request.targetCardinality());
        UmlRelation saved = relations.save(relation);
        collaborationService.publishDiagramChanged(saved.getDiagramId(), "actualizó cardinalidades");
        log.info("Cardinalidades actualizadas para la relación UML: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional public void deleteRelation(UUID id) {
        UmlRelation relation = findRelation(id);
        relations.delete(relation);
        collaborationService.publishDiagramChanged(relation.getDiagramId(), "eliminó una relación");
    }
    private Diagram findDiagram(UUID id) {
        Diagram diagram = diagrams.findById(id).orElseThrow(() -> new ResourceNotFoundException("Diagrama", id));
        projectService.findEditableEntity(diagram.getProjectId());
        return diagram;
    }
    private void verifyExpectedVersion(String resource, Long actualVersion, Long expectedVersion) {
        if (!expectedVersion.equals(actualVersion)) throw new VersionConflictException(resource);
    }
    private UmlClass findClass(UUID id) {
        UmlClass umlClass = classes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Clase UML", id));
        findDiagram(umlClass.getDiagramId());
        return umlClass;
    }
    private UmlAttribute findAttribute(UUID id) {
        UmlAttribute attribute = attributes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Atributo UML", id));
        findClass(attribute.getUmlClassId());
        return attribute;
    }
    private UmlOperation findOperation(UUID id) {
        UmlOperation operation = operations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Operación UML", id));
        findClass(operation.getUmlClassId());
        return operation;
    }
    private void applyOperationValues(UmlOperation operation, String name, String visibility, OperationReturnType returnType) {
        operation.setName(name);
        operation.setVisibility(visibility);
        operation.setReturnType(returnType.displayName());
    }
    private void saveOperationParameters(UUID operationId, List<UmlOperationParameterRequest> requests) {
        for (int index = 0; index < requests.size(); index++) {
            UmlOperationParameterRequest request = requests.get(index);
            UmlOperationParameter parameter = new UmlOperationParameter();
            parameter.setUmlOperationId(operationId);
            parameter.setName(request.name());
            parameter.setDataType(request.dataType().displayName());
            parameter.setParameterOrder(index);
            operationParameters.save(parameter);
        }
    }
    private void validateParameterNames(List<UmlOperationParameterRequest> parameters) {
        long distinctNames = parameters.stream().map(parameter -> parameter.name().trim().toLowerCase()).distinct().count();
        if (distinctNames != parameters.size()) {
            throw new IllegalArgumentException("Los parámetros de una operación no pueden repetir nombre.");
        }
    }
    private UmlRelation findRelation(UUID id) {
        UmlRelation relation = relations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Relación UML", id));
        findDiagram(relation.getDiagramId());
        return relation;
    }
    private void applyRelationValues(UmlRelation relation, UUID sourceClassId, UUID targetClassId, RelationType type, String label, String sourceCardinality, String targetCardinality, Double bendX, Double bendY, UUID associationClassId, List<RelationAlignmentPoint> alignmentPoints) {
        UmlClass source = findClass(sourceClassId);
        UmlClass target = findClass(targetClassId);
        if (!source.getDiagramId().equals(relation.getDiagramId()) || !target.getDiagramId().equals(relation.getDiagramId())) {
            throw new IllegalArgumentException("Las clases deben pertenecer al mismo diagrama.");
        }
        relation.setSourceClassId(sourceClassId);
        relation.setTargetClassId(targetClassId);
        relation.setType(type);
        relation.setLabel(supportsLabel(type) ? label : null);
        if ((bendX == null) != (bendY == null)) throw new IllegalArgumentException("El punto de quiebre requiere coordenadas X e Y.");
        relation.setBendX(bendX);
        relation.setBendY(bendY);
        relation.setAlignmentPoints(serializeAlignmentPoints(alignmentPoints));
        if (associationClassId != null) {
            if (type != RelationType.ASSOCIATION) throw new IllegalArgumentException("La clase de asociación solo puede vincularse a una asociación.");
            if (associationClassId.equals(sourceClassId) || associationClassId.equals(targetClassId)) {
                throw new IllegalArgumentException("La clase de asociación debe ser distinta de las clases conectadas.");
            }
            UmlClass associationClass = findClass(associationClassId);
            if (!associationClass.getDiagramId().equals(relation.getDiagramId())) throw new IllegalArgumentException("La clase de asociación debe pertenecer al mismo diagrama.");
        }
        relation.setAssociationClassId(associationClassId);
        if (supportsCardinality(type) && associationClassId == null) {
            if (sourceCardinality == null || targetCardinality == null) {
                throw new IllegalArgumentException("La relación requiere cardinalidad en ambos extremos.");
            }
            relation.setSourceCardinality(sourceCardinality);
            relation.setTargetCardinality(targetCardinality);
        } else {
            relation.setSourceCardinality(null);
            relation.setTargetCardinality(null);
        }
    }
    private boolean supportsLabel(RelationType type) { return type == RelationType.ASSOCIATION || type == RelationType.AGGREGATION || type == RelationType.COMPOSITION || type == RelationType.DEPENDENCY; }
    private String serializeAlignmentPoints(List<RelationAlignmentPoint> points) {
        if (points == null || points.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(points); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Los puntos de alineación no son válidos.", exception); }
    }
    private List<RelationAlignmentPoint> deserializeAlignmentPoints(String points) {
        if (points == null || points.isBlank()) return List.of();
        try { return objectMapper.readValue(points, new TypeReference<List<RelationAlignmentPoint>>() {}); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("No se pudieron leer los puntos de alineación.", exception); }
    }
    private DiagramResponse toResponse(Diagram item) { return new DiagramResponse(item.getId(), item.getProjectId(), item.getName(), item.getVersion(), item.getCreatedAt()); }
    private DiagramDrawingResponse toResponse(DiagramDrawing item) { return new DiagramDrawingResponse(item.getId(), item.getSvgPath()); }
    private boolean supportsCardinality(RelationType type) { return type == RelationType.ASSOCIATION || type == RelationType.AGGREGATION || type == RelationType.COMPOSITION; }
    private UmlClassResponse toResponse(UmlClass item) { return new UmlClassResponse(item.getId(), item.getDiagramId(), item.getName(), item.getPositionX(), item.getPositionY(), item.getFillColor(), item.getVersion(), attributes.findByUmlClassId(item.getId()).stream().map(this::toResponse).toList(), operations.findByUmlClassId(item.getId()).stream().map(this::toResponse).toList()); }
    private UmlAttributeResponse toResponse(UmlAttribute item) { return new UmlAttributeResponse(item.getId(), item.getUmlClassId(), item.getName(), item.getDataType(), item.getVisibility()); }
    private UmlOperationResponse toResponse(UmlOperation item) { return new UmlOperationResponse(item.getId(), item.getUmlClassId(), item.getName(), item.getVisibility(), item.getReturnType(), operationParameters.findByUmlOperationIdOrderByParameterOrderAsc(item.getId()).stream().map(parameter -> new UmlOperationParameterResponse(parameter.getId(), parameter.getName(), parameter.getDataType(), parameter.getParameterOrder())).toList()); }
    private UmlRelationResponse toResponse(UmlRelation item) { return new UmlRelationResponse(item.getId(), item.getDiagramId(), item.getSourceClassId(), item.getTargetClassId(), item.getType(), item.getLabel(), item.getSourceCardinality(), item.getTargetCardinality(), item.getBendX(), item.getBendY(), item.getAssociationClassId(), deserializeAlignmentPoints(item.getAlignmentPoints())); }
}
