package com.examensw1.umlcollab.features.generation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examensw1.umlcollab.features.auth.service.AuthService;
import com.examensw1.umlcollab.features.generation.service.BackendGenerationService;
import com.examensw1.umlcollab.features.generation.service.FlutterGenerationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BackendGenerationController.class)
@AutoConfigureMockMvc(addFilters = false)
class BackendGenerationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private BackendGenerationService service;
    @MockitoBean private FlutterGenerationService flutterGenerationService;
    @MockitoBean private AuthService authService;

    @Test
    void downloadsGeneratedBackendAsZip() throws Exception {
        UUID diagramId = UUID.randomUUID();
        when(service.generate(diagramId)).thenReturn(new BackendGenerationService.GeneratedBackend("biblioteca-backend.zip", new byte[] {80, 75}));

        mockMvc.perform(get("/api/diagrams/{diagramId}/generate/backend", diagramId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=biblioteca-backend.zip"));
    }

    @Test
    void downloadsGeneratedFlutterAsZip() throws Exception {
        UUID diagramId = UUID.randomUUID();
        when(flutterGenerationService.generate(diagramId)).thenReturn(new FlutterGenerationService.GeneratedFlutter("biblioteca-flutter.zip", new byte[] {80, 75}));

        mockMvc.perform(get("/api/diagrams/{diagramId}/generate/flutter", diagramId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=biblioteca-flutter.zip"));
    }
}
