package com.examensw1.umlcollab.features.collaboration.controller;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEvent;
import com.examensw1.umlcollab.features.collaboration.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
@Controller @RequiredArgsConstructor
public class DiagramWebSocketController {
    private final CollaborationService service;
    @MessageMapping("/diagram-events") @SendTo("/topic/diagram-events")
    public DiagramEvent broadcast(DiagramEvent event) { return service.publish(event); }
}
