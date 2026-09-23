package com.examensw1.umlcollab.features.diagram.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.diagram.dto.AssistantCommandResponse;
import com.examensw1.umlcollab.features.diagram.dto.CreateAttributeRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateUmlClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateUmlOperationRequest;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.dto.ExecuteAssistantCommandRequest;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UpdateAttributeRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateRelationCardinalityRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateUmlClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateUmlOperationRequest;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagramAssistantServiceTest {
    @Mock private DiagramService diagramService;
    private DiagramAssistantService assistant;
    private final UUID diagramId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final UUID attributeId = UUID.randomUUID();
    private final UUID operationId = UUID.randomUUID();
    private final UUID relationId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        assistant = new DiagramAssistantService(diagramService, new DiagramAssistantPlanner(diagramService));
        UmlAttributeResponse attribute = new UmlAttributeResponse(attributeId, userId, "nombre", "String", "PRIVATE", false);
        UmlOperationResponse operation = new UmlOperationResponse(operationId, userId, "registrar", "PUBLIC", "void", List.of());
        UmlClassResponse user = new UmlClassResponse(userId, diagramId, "Usuario", 100, 100, null, 0L, List.of(attribute), List.of(operation));
        UmlClassResponse client = new UmlClassResponse(clientId, diagramId, "Cliente", 300, 100, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(relationId, diagramId, userId, clientId, RelationType.ASSOCIATION, null, "1..1", "1..*");
        DiagramDetailsResponse details = new DiagramDetailsResponse(new DiagramResponse(diagramId, UUID.randomUUID(), "Modelo", 0L, Instant.now()), List.of(user, client), List.of(relation), List.of());
        when(diagramService.getDetails(diagramId)).thenReturn(details);
    }

    private AssistantCommandResponse run(String text, boolean confirmed) {
        return assistant.execute(diagramId, new ExecuteAssistantCommandRequest(text, confirmed));
    }

    @Test
    void previewsCreateWithoutMutationAndExecutesOnlyAfterConfirmation() {
        assertTrue(run("crear clase Pago", false).requiresConfirmation());
        verify(diagramService, never()).createClass(eq(diagramId), any());
        assertFalse(run("crear clase Pago", true).requiresConfirmation());
        verify(diagramService).createClass(eq(diagramId), any(CreateUmlClassRequest.class));
    }

    @Test
    void movesRenamesAndDeletesExistingClass() {
        run("mover clase Usuario a 200, 300", true);
        verify(diagramService).updateClass(userId, new UpdateUmlClassRequest("Usuario", 200, 300, null));
        run("renombrar clase Usuario a Persona", true);
        verify(diagramService).updateClass(userId, new UpdateUmlClassRequest("Persona", 100, 100, null));
        assertTrue(run("eliminar clase Usuario", false).requiresConfirmation());
        run("eliminar clase Usuario", true);
        verify(diagramService).deleteClass(userId);
    }

    @Test
    void supportsAttributeCrud() {
        run("agrega a Usuario un atributo llamado edad de tipo Integer", true);
        verify(diagramService).createAttribute(eq(userId), any(CreateAttributeRequest.class));
        run("renombrar atributo nombre de clase Usuario a nombreCompleto", true);
        verify(diagramService).updateAttribute(eq(attributeId), any(UpdateAttributeRequest.class));
        run("eliminar atributo nombre de clase Usuario", true);
        verify(diagramService).deleteAttribute(attributeId);
        assertEquals("READ_ATTRIBUTES", run("mostrar atributos de Usuario", false).action());
    }

    @Test
    void deletesAttributeWhenUserCallsItsClassATableAndEndsWithPeriod() {
        DiagramDetailsResponse original = diagramService.getDetails(diagramId);
        UUID clientAttributeId = UUID.randomUUID();
        UmlAttributeResponse clientAttribute = new UmlAttributeResponse(clientAttributeId, clientId, "nombre", "String", "PRIVATE", false);
        UmlClassResponse client = new UmlClassResponse(clientId, diagramId, "Cliente", 300, 100, null, 0L, List.of(clientAttribute), List.of());
        when(diagramService.getDetails(diagramId)).thenReturn(new DiagramDetailsResponse(
                original.diagram(), List.of(original.classes().getFirst(), client), original.relations(), List.of()));

        String command = "Eliminar el atributo nombre de la tabla Cliente.";
        AssistantCommandResponse preview = run(command, false);
        assertEquals("DELETE_ATTRIBUTE", preview.action());
        assertTrue(preview.requiresConfirmation());
        verify(diagramService, never()).deleteAttribute(any());

        run(command, true);
        verify(diagramService).deleteAttribute(clientAttributeId);
        verify(diagramService, never()).deleteAttribute(attributeId);
    }

    @Test
    void supportsOperationCrud() {
        run("crear operación buscar en clase Usuario", true);
        verify(diagramService).createOperation(eq(userId), any(CreateUmlOperationRequest.class));
        run("renombrar operación registrar de clase Usuario a guardar", true);
        verify(diagramService).updateOperation(eq(operationId), any(UpdateUmlOperationRequest.class));
        run("eliminar operación registrar de clase Usuario", true);
        verify(diagramService).deleteOperation(operationId);
        assertEquals("READ_OPERATIONS", run("mostrar operaciones de Usuario", false).action());
    }

    @Test
    void supportsRelationCrud() {
        run("crear asociación entre Usuario y Cliente", true);
        verify(diagramService).createRelation(eq(diagramId), any(CreateRelationRequest.class));
        run("cambiar cardinalidades de relación entre Usuario y Cliente a 0..1 y 0..*", true);
        verify(diagramService).updateRelationCardinality(relationId, new UpdateRelationCardinalityRequest("0..1", "0..*"));
        assertTrue(run("eliminar relación entre Usuario y Cliente", false).requiresConfirmation());
        run("eliminar relación entre Usuario y Cliente", true);
        verify(diagramService).deleteRelation(relationId);
        assertEquals("READ_RELATIONS", run("listar relaciones", false).action());
    }

    @Test
    void preservesRequestedCardinalitiesAndEndpointDirection() {
        run("crear asociación entre Usuario y Cliente con cardinalidades 0..1 y 0..*", true);
        verify(diagramService).createRelation(diagramId,
                new CreateRelationRequest(userId, clientId, RelationType.ASSOCIATION, null, "0..1", "0..*"));
        run("cambiar cardinalidades de relación entre Cliente y Usuario a 0..* y 0..1", true);
        verify(diagramService).updateRelationCardinality(relationId, new UpdateRelationCardinalityRequest("0..1", "0..*"));
    }

    @Test
    void rejectsAmbiguousRelationInsteadOfDeletingAnArbitraryOne() {
        DiagramDetailsResponse original = diagramService.getDetails(diagramId);
        UmlRelationResponse second = new UmlRelationResponse(UUID.randomUUID(), diagramId, userId, clientId,
                RelationType.DEPENDENCY, null, null, null);
        when(diagramService.getDetails(diagramId)).thenReturn(new DiagramDetailsResponse(
                original.diagram(), original.classes(), List.of(original.relations().getFirst(), second), List.of()));

        assertThrows(IllegalArgumentException.class, () -> run("eliminar relación entre Usuario y Cliente", true));
        verify(diagramService, never()).deleteRelation(any());
        run("eliminar asociación entre Usuario y Cliente", true);
        verify(diagramService).deleteRelation(relationId);
    }

    @Test
    void rejectsUnknownAndOutOfBoundsCommandsWithoutMutation() {
        assertThrows(IllegalArgumentException.class, () -> run("ejecuta SQL", true));
        assertThrows(IllegalArgumentException.class, () -> run("mover clase Usuario a 99999, 2", true));
        assertThrows(IllegalArgumentException.class, () -> run("eliminar clase Desconocida", true));
        verify(diagramService, never()).deleteClass(any());
    }
}
