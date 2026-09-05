package com.examensw1.umlcollab.features.collaboration.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.examensw1.umlcollab.features.collaboration.dto.DiagramEvent;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEventType;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiagramWebSocketControllerTest {

    @Mock
    private CollaborationService collaborationService;

    @InjectMocks
    private DiagramWebSocketController controller;

    @Test
    void debeRegistrarPresenciaCuandoElClienteSeUneAlDiagrama() {
        UUID diagramId = UUID.randomUUID();
        Principal principal = () -> "tatiana@example.com";

        controller.handle(new DiagramEvent(diagramId, DiagramEventType.PRESENCE_JOINED, null, null), principal);

        verify(collaborationService).registerPresence(diagramId, principal.getName());
    }

    @Test
    void debeRechazarEventoDeCambioEnviadoPorElCliente() {
        Principal principal = () -> "tatiana@example.com";

        assertThrows(IllegalArgumentException.class, () -> controller.handle(
                new DiagramEvent(UUID.randomUUID(), DiagramEventType.DIAGRAM_CHANGED, null, null), principal));

        verifyNoInteractions(collaborationService);
    }

    @Test
    void debeReenviarPrevisualizacionDeTrazoParaQueElServicioLaAutorice() {
        UUID diagramId = UUID.randomUUID();
        Principal principal = () -> "tatiana@example.com";

        controller.handle(new DiagramEvent(diagramId, DiagramEventType.DRAWING_PREVIEW, null, null), principal);

        verify(collaborationService).publishEphemeralEvent(any(), any(), any(), any());
    }
}
