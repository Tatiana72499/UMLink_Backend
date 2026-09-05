package com.examensw1.umlcollab.features.collaboration.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.collaboration.dto.DiagramActivityResponse;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagramActivityControllerTest {

    @Mock private CollaborationService collaborationService;
    @InjectMocks private DiagramActivityController controller;

    @Test
    void debeDevolverElHistorialDelDiagrama() {
        UUID diagramId = UUID.randomUUID();
        DiagramActivityResponse event = new DiagramActivityResponse(UUID.randomUUID(), UUID.randomUUID(),
                "Tatiana", "creó una clase", Instant.now());
        when(collaborationService.findActivity(diagramId)).thenReturn(List.of(event));

        List<DiagramActivityResponse> response = controller.list(diagramId);

        assertEquals(List.of(event), response);
    }
}
