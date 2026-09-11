package com.examensw1.umlcollab.features.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examensw1.umlcollab.features.ai.dto.DiagramImagePreviewResponse;
import com.examensw1.umlcollab.features.ai.service.DiagramImageAnalysisService;
import com.examensw1.umlcollab.features.auth.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiagramImageAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiagramImageAnalysisControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private DiagramImageAnalysisService service;
    @MockitoBean private AuthService authService;

    @Test
    void debeDevolverVistaPreviaParaImagenValida() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(service.preview(eq(projectId), any(), eq("image/png")))
                .thenReturn(new DiagramImagePreviewResponse("@startuml\nclass Usuario\n@enduml", "Diagrama desde imagen", 1, 0));
        MockMultipartFile file = new MockMultipartFile("file", "modelo.png", "image/png", new byte[] {1, 2});

        mockMvc.perform(multipart("/api/projects/{projectId}/diagrams/ai/image-preview", projectId).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classCount").value(1))
                .andExpect(jsonPath("$.plantUml").value(org.hamcrest.Matchers.containsString("class Usuario")));
    }
}
