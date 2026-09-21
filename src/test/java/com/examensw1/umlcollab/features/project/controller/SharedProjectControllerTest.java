package com.examensw1.umlcollab.features.project.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examensw1.umlcollab.config.CorsConfig;
import com.examensw1.umlcollab.features.auth.service.AuthService;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SharedProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CorsConfig.class)
class SharedProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private DiagramService diagramService;

    @MockitoBean
    private AuthService authService;

    @Test
    void debeExponerUnProyectoMedianteTokenCompartido() throws Exception {
        UUID shareToken = UUID.randomUUID();
        ProjectResponse project = new ProjectResponse(UUID.randomUUID(), "Biblioteca", "Modelo UML", "Tatiana", 0L,
                Instant.parse("2026-09-02T00:00:00Z"));
        when(projectService.findSharedByToken(shareToken)).thenReturn(project);

        mockMvc.perform(get("/api/shared/projects/{shareToken}", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Biblioteca"));
    }

    @Test
    void debeListarDiagramasMedianteTokenCompartido() throws Exception {
        UUID shareToken = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(UUID.randomUUID(), UUID.randomUUID(), "Dominio", 0L,
                Instant.parse("2026-09-02T00:00:00Z"));
        when(diagramService.findSharedByToken(shareToken)).thenReturn(List.of(diagram));

        mockMvc.perform(get("/api/shared/projects/{shareToken}/diagrams", shareToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dominio"));
    }

    @Test
    void debeConsultarSoloElDetalleDelDiagramaAlcanzablePorElToken() throws Exception {
        UUID shareToken = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID diagramId = UUID.randomUUID();
        DiagramResponse diagram = new DiagramResponse(diagramId, projectId, "Dominio", 0L,
                Instant.parse("2026-09-02T00:00:00Z"));
        when(diagramService.getSharedDetails(shareToken, diagramId))
                .thenReturn(new DiagramDetailsResponse(diagram, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/shared/projects/{shareToken}/diagrams/{diagramId}", shareToken, diagramId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagram.id").value(diagramId.toString()));
    }
}
