package com.examensw1.umlcollab.features.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.diagram.service.UmlInterchangeService;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagramImageAnalysisServiceTest {
    @Mock private ProjectService projectService;
    @Mock private DiagramImageAiClient imageAiClient;
    private final UmlInterchangeService interchangeService = new UmlInterchangeService();

    @Test
    void debeCrearVistaPreviaSinPersistirCuandoLaImagenEsValida() {
        UUID projectId = UUID.randomUUID();
        var service = new DiagramImageAnalysisService(projectService, imageAiClient, interchangeService);
        when(imageAiClient.generatePlantUml(any(), anyString())).thenReturn("@startuml\nclass Usuario {\n- id : String\n}\n@enduml");

        var result = service.preview(projectId, new byte[] {1, 2}, "image/png");

        assertThat(result.classCount()).isEqualTo(1);
        assertThat(result.relationCount()).isZero();
        assertThat(result.plantUml()).contains("class Usuario");
        verify(projectService).findEditableEntity(projectId);
    }

    @Test
    void debeRechazarImagenConTipoNoPermitidoAntesDeLlamarAIA() {
        var service = new DiagramImageAnalysisService(projectService, imageAiClient, interchangeService);

        assertThatThrownBy(() -> service.preview(UUID.randomUUID(), new byte[] {1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PNG, JPG o WEBP");
    }
}
