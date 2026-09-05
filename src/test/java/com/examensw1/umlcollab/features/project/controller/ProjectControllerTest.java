package com.examensw1.umlcollab.features.project.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examensw1.umlcollab.config.CorsConfig;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.auth.service.AuthService;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.dto.ProjectMemberResponse;
import com.examensw1.umlcollab.features.project.model.ProjectRole;
import com.examensw1.umlcollab.features.project.service.ProjectService;
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

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CorsConfig.class)
class ProjectControllerTest {

    private static final String ANGULAR_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private AuthService authService;

    @Test
    void debeListarProyectosYPermitirOrigenAngular() throws Exception {
        ProjectResponse project = new ProjectResponse(UUID.randomUUID(), "Biblioteca", "Modelo UML", "Tatiana", 0L, Instant.parse("2026-09-02T00:00:00Z"));
        when(projectService.findAll()).thenReturn(List.of(project));

        mockMvc.perform(get("/api/projects").header("Origin", ANGULAR_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ANGULAR_ORIGIN))
                .andExpect(jsonPath("$[0].name").value("Biblioteca"));
    }

    @Test
    void debeCrearProyectoDesdeAngular() throws Exception {
        ProjectResponse project = new ProjectResponse(UUID.randomUUID(), "Biblioteca", "Modelo UML", "Tatiana", 0L, Instant.parse("2026-09-02T00:00:00Z"));
        when(projectService.create(any())).thenReturn(project);

        mockMvc.perform(post("/api/projects")
                        .header("Origin", ANGULAR_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Biblioteca\",\"description\":\"Modelo UML\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", ANGULAR_ORIGIN))
                .andExpect(jsonPath("$.ownerName").value("Tatiana"));
    }

    @Test
    void debeResponderPreflightParaAngular() throws Exception {
        mockMvc.perform(options("/api/projects")
                        .header("Origin", ANGULAR_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ANGULAR_ORIGIN));
    }

    @Test
    void debeResponderErroresDeValidacionPorCampo() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void debeResponderConflictoDeVersionAlActualizarProyecto() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.update(eq(projectId), any())).thenThrow(new VersionConflictException("El proyecto"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Biblioteca\",\"description\":\"Modelo\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void debeListarMiembrosDelProyecto() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectMemberResponse member = new ProjectMemberResponse(UUID.randomUUID(), UUID.randomUUID(), "Tatiana",
                "tatiana@umlink.dev", ProjectRole.OWNER);
        when(projectService.members(projectId)).thenReturn(List.of(member));

        mockMvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void debeValidarElCorreoAlInvitarMiembro() throws Exception {
        mockMvc.perform(post("/api/projects/{id}/members", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"role\":\"EDITOR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }
}
