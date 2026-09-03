package com.examensw1.umlcollab.features.diagram.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.diagram.dto.CreateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateAssociationClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.AssociationClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UpdateRelationCardinalityRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateDiagramRequest;
import com.examensw1.umlcollab.features.diagram.dto.RelationAlignmentPoint;
import com.examensw1.umlcollab.features.diagram.model.Diagram;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import com.examensw1.umlcollab.features.diagram.model.UmlClass;
import com.examensw1.umlcollab.features.diagram.model.UmlRelation;
import com.examensw1.umlcollab.features.diagram.repository.DiagramRepository;
import com.examensw1.umlcollab.features.diagram.repository.UmlAttributeRepository;
import com.examensw1.umlcollab.features.diagram.repository.UmlClassRepository;
import com.examensw1.umlcollab.features.diagram.repository.UmlRelationRepository;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagramServiceTest {

    @Mock private ProjectService projects;
    @Mock private DiagramRepository diagrams;
    @Mock private UmlClassRepository classes;
    @Mock private UmlAttributeRepository attributes;
    @Mock private UmlRelationRepository relations;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private DiagramService service;

    private final UUID diagramId = UUID.randomUUID();
    private final UUID sourceClassId = UUID.randomUUID();
    private final UUID targetClassId = UUID.randomUUID();

    @BeforeEach
    void prepareDiagramAndClasses() {
        Diagram diagram = new Diagram();
        diagram.setId(diagramId);
        when(diagrams.findById(diagramId)).thenReturn(Optional.of(diagram));
    }

    private void stubClassesInDiagram() {
        when(classes.findById(sourceClassId)).thenReturn(Optional.of(umlClass(sourceClassId)));
        when(classes.findById(targetClassId)).thenReturn(Optional.of(umlClass(targetClassId)));
    }

    @Test
    void debeGuardarCardinalidadesEnUnaAsociacion() {
        stubClassesInDiagram();
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UmlRelationResponse response = service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.ASSOCIATION, "posee", "1..1", "1..*"));

        assertEquals("1..1", response.sourceCardinality());
        assertEquals("1..*", response.targetCardinality());
    }

    @Test
    void debeRechazarAsociacionSinCardinalidades() {
        stubClassesInDiagram();
        assertThrows(IllegalArgumentException.class, () -> service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.ASSOCIATION, null, null, null)));
    }

    @Test
    void debeCrearGeneralizacionSinCardinalidad() {
        stubClassesInDiagram();
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UmlRelationResponse response = service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.GENERALIZATION, null, null, null));

        assertNull(response.sourceCardinality());
        assertNull(response.targetCardinality());
    }

    @Test
    void debeActualizarCardinalidadesDeUnaRelacion() {
        UUID relationId = UUID.randomUUID();
        UmlRelation relation = new UmlRelation();
        relation.setId(relationId);
        relation.setDiagramId(diagramId);
        relation.setType(RelationType.ASSOCIATION);
        when(relations.findById(relationId)).thenReturn(Optional.of(relation));
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UmlRelationResponse response = service.updateRelationCardinality(relationId,
                new UpdateRelationCardinalityRequest("0..1", "1..*"));

        assertEquals("0..1", response.sourceCardinality());
        assertEquals("1..*", response.targetCardinality());
    }

    @Test
    void debeRechazarCardinalidadEnUnaGeneralizacion() {
        UUID relationId = UUID.randomUUID();
        UmlRelation relation = new UmlRelation();
        relation.setId(relationId);
        relation.setDiagramId(diagramId);
        relation.setType(RelationType.GENERALIZATION);
        when(relations.findById(relationId)).thenReturn(Optional.of(relation));

        assertThrows(IllegalArgumentException.class, () -> service.updateRelationCardinality(relationId,
                new UpdateRelationCardinalityRequest("1", "1")));
    }

    @Test
    void debeActualizarTipoYExtremosDeUnaRelacion() {
        UUID relationId = UUID.randomUUID();
        UmlRelation relation = new UmlRelation();
        relation.setId(relationId);
        relation.setDiagramId(diagramId);
        relation.setType(RelationType.ASSOCIATION);
        when(relations.findById(relationId)).thenReturn(Optional.of(relation));
        stubClassesInDiagram();
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UmlRelationResponse response = service.updateRelation(relationId,
                new UpdateRelationRequest(sourceClassId, targetClassId, RelationType.DEPENDENCY, null, null, null));

        assertEquals(RelationType.DEPENDENCY, response.type());
        assertNull(response.sourceCardinality());
    }

    @Test
    void debeVincularUnaClaseIntermediaAUnaAsociacion() {
        UUID associationClassId = UUID.randomUUID();
        stubClassesInDiagram();
        when(classes.findById(associationClassId)).thenReturn(Optional.of(umlClass(associationClassId)));
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UmlRelationResponse response = service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.ASSOCIATION,
                        "matricula", "1..1", "1..*", null, null, associationClassId));

        assertEquals(associationClassId, response.associationClassId());
    }

    @Test
    void debeRechazarClaseIntermediaQueEsExtremoDeLaAsociacion() {
        stubClassesInDiagram();

        assertThrows(IllegalArgumentException.class, () -> service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.ASSOCIATION,
                        null, "1..1", "1..*", null, null, sourceClassId)));
    }

    @Test
    void debeCrearAsociacionYClaseIntermediaEnUnaOperacion() {
        stubClassesInDiagram();
        UUID associationClassId = UUID.randomUUID();
        when(classes.save(any(UmlClass.class))).thenAnswer(invocation -> {
            UmlClass saved = invocation.getArgument(0);
            saved.setId(associationClassId);
            return saved;
        });
        when(classes.findById(associationClassId)).thenAnswer(invocation -> Optional.of(umlClass(associationClassId)));
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssociationClassResponse response = service.createAssociationClass(diagramId,
                new CreateAssociationClassRequest(sourceClassId, targetClassId, "Inscripción", 300, 220,
                        "#EAF3FF", null));

        assertEquals("Inscripción", response.umlClass().name());
        assertEquals(associationClassId, response.relation().associationClassId());
        assertNull(response.relation().sourceCardinality());
    }

    @Test
    void debeGuardarVariosPuntosDeAlineacionEnUnaRelacion() {
        stubClassesInDiagram();
        when(relations.save(any(UmlRelation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<RelationAlignmentPoint> points = List.of(new RelationAlignmentPoint(150, 200), new RelationAlignmentPoint(300, 260));

        UmlRelationResponse response = service.createRelation(diagramId,
                new CreateRelationRequest(sourceClassId, targetClassId, RelationType.ASSOCIATION,
                        null, "1..1", "1..*", null, null, null, points));

        assertEquals(points, response.alignmentPoints());
    }

    @Test
    void debeActualizarDiagramaConLaVersionActual() {
        Diagram diagram = new Diagram();
        diagram.setId(diagramId);
        diagram.setVersion(0L);
        when(diagrams.findById(diagramId)).thenReturn(Optional.of(diagram));
        when(diagrams.saveAndFlush(diagram)).thenReturn(diagram);

        service.updateDiagram(diagramId, new UpdateDiagramRequest("Dominio", 0L));

        assertEquals("Dominio", diagram.getName());
    }

    @Test
    void debeRechazarDiagramaConVersionDesactualizada() {
        Diagram diagram = new Diagram();
        diagram.setId(diagramId);
        diagram.setVersion(2L);
        when(diagrams.findById(diagramId)).thenReturn(Optional.of(diagram));

        assertThrows(VersionConflictException.class,
                () -> service.updateDiagram(diagramId, new UpdateDiagramRequest("Dominio", 1L)));
    }

    private UmlClass umlClass(UUID id) {
        UmlClass umlClass = new UmlClass();
        umlClass.setId(id);
        umlClass.setDiagramId(diagramId);
        return umlClass;
    }
}
