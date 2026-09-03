package com.examensw1.umlcollab.features.diagram.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examensw1.umlcollab.config.CorsConfig;
import com.examensw1.umlcollab.features.auth.service.AuthService;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiagramController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CorsConfig.class)
class DiagramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private AuthService authService;

    @Test
    void debeListarDiagramasDeUnProyecto() throws Exception {
        UUID projectId = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(UUID.randomUUID(), projectId, "Dominio", 0L, Instant.parse("2026-09-02T00:00:00Z"));
        when(diagramService.findByProject(projectId)).thenReturn(List.of(diagram));

        mockMvc.perform(get("/api/projects/{projectId}/diagrams", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dominio"));
    }

    @Test
    void debeCrearDiagramaConSolicitudValida() throws Exception {
        UUID projectId = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(UUID.randomUUID(), projectId, "Dominio", 0L, Instant.parse("2026-09-02T00:00:00Z"));
        when(diagramService.createDiagram(eq(projectId), any())).thenReturn(diagram);

        mockMvc.perform(post("/api/projects/{projectId}/diagrams", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dominio\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Dominio"));
    }

    @Test
    void debeObtenerDetalleDeDiagrama() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID diagramId = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(diagramId, projectId, "Dominio", 0L, Instant.parse("2026-09-02T00:00:00Z"));
        DiagramDetailsResponse details = new DiagramDetailsResponse(diagram, List.of(), List.of());
        when(diagramService.getDetails(diagramId)).thenReturn(details);

        mockMvc.perform(get("/api/diagrams/{diagramId}", diagramId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagram.id").value(diagramId.toString()));
    }

    @Test
    void debeCrearClaseConSolicitudValida() throws Exception {
        UUID diagramId = UUID.randomUUID();
        UmlClassResponse umlClass = new UmlClassResponse(UUID.randomUUID(), diagramId, "Usuario", 100, 120, "#EAF3FF", 0L, List.of());
        when(diagramService.createClass(eq(diagramId), any())).thenReturn(umlClass);

        mockMvc.perform(post("/api/diagrams/{diagramId}/classes", diagramId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Usuario\",\"positionX\":100,\"positionY\":120}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Usuario"));
    }

    @Test
    void debeActualizarCardinalidadDeRelacion() throws Exception {
        UUID relationId = UUID.randomUUID();
        UmlRelationResponse relation = new UmlRelationResponse(relationId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), RelationType.ASSOCIATION, "posee", "0..1", "1..*");
        when(diagramService.updateRelationCardinality(eq(relationId), any())).thenReturn(relation);

        mockMvc.perform(put("/api/relations/{id}/cardinality", relationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceCardinality\":\"0..1\",\"targetCardinality\":\"1..*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCardinality").value("0..1"))
                .andExpect(jsonPath("$.targetCardinality").value("1..*"));
    }

    @Test
    void debeActualizarAtributoConSolicitudValida() throws Exception {
        UUID attributeId = UUID.randomUUID();
        UmlAttributeResponse attribute = new UmlAttributeResponse(attributeId, UUID.randomUUID(), "email", "String", "PRIVATE");
        when(diagramService.updateAttribute(eq(attributeId), any())).thenReturn(attribute);

        mockMvc.perform(put("/api/attributes/{id}", attributeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"email\",\"dataType\":\"STRING\",\"visibility\":\"PRIVATE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("email"));
    }

    @Test
    void debeActualizarDiagramaConVersionActual() throws Exception {
        UUID diagramId = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(diagramId, UUID.randomUUID(), "Dominio actualizado", 1L, Instant.parse("2026-09-02T00:00:00Z"));
        when(diagramService.updateDiagram(eq(diagramId), any())).thenReturn(diagram);

        mockMvc.perform(put("/api/diagrams/{diagramId}", diagramId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dominio actualizado\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void debeEliminarDiagramaConVersionActual() throws Exception {
        UUID diagramId = UUID.randomUUID();

        mockMvc.perform(delete("/api/diagrams/{diagramId}", diagramId).param("version", "0"))
                .andExpect(status().isNoContent());
    }
}
