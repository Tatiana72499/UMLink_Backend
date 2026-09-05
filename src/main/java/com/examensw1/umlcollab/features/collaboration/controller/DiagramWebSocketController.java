package com.examensw1.umlcollab.features.collaboration.controller;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEvent;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEventType;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DiagramWebSocketController {
    private final CollaborationService service;

    @MessageMapping("/diagram-events")
    public void handle(@Valid DiagramEvent event, Principal principal) {
        if (event.type() == DiagramEventType.PRESENCE_JOINED) {
            service.registerPresence(event.diagramId(), principal.getName());
            return;
        }
        if (event.type() == DiagramEventType.PRESENCE_LEFT) {
            service.unregisterPresence(event.diagramId(), principal.getName());
            return;
        }
        if (event.type() == DiagramEventType.DIAGRAM_CHANGED) {
            throw new IllegalArgumentException("Este tipo de evento solo puede ser publicado por el servidor.");
        }
        if (event.type() == DiagramEventType.DRAWING_PREVIEW
                || event.type() == DiagramEventType.DRAWING_PREVIEW_CLEARED
                || event.type() == DiagramEventType.ELEMENT_INTERACTION) {
            service.publishEphemeralEvent(event.diagramId(), principal.getName(), event.type(), event.payload());
        }
    }
}
