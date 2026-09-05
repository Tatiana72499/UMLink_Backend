package com.examensw1.umlcollab.features.collaboration.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.repository.AppUserRepository;
import com.examensw1.umlcollab.features.auth.service.CurrentUserService;
import com.examensw1.umlcollab.features.collaboration.dto.CollaborationParticipant;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramActivityResponse;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEvent;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEventType;
import com.examensw1.umlcollab.features.collaboration.model.DiagramActivityEvent;
import com.examensw1.umlcollab.features.collaboration.repository.DiagramActivityEventRepository;
import com.examensw1.umlcollab.features.diagram.model.Diagram;
import com.examensw1.umlcollab.features.diagram.repository.DiagramRepository;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.model.ProjectMember;
import com.examensw1.umlcollab.features.project.model.ProjectRole;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import com.examensw1.umlcollab.features.project.repository.ProjectMemberRepository;
import java.util.UUID;
import java.util.Set;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollaborationService {

    private final AppUserRepository users;
    private final DiagramRepository diagrams;
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DiagramActivityEventRepository activityEvents;

    public void registerPresence(UUID diagramId, String email) {
        AppUser user = requireMember(diagramId, email);
        publish(diagramId, DiagramEventType.PRESENCE_JOINED, participant(user));
    }

    public void unregisterPresence(UUID diagramId, String email) {
        AppUser user = requireMember(diagramId, email);
        publish(diagramId, DiagramEventType.PRESENCE_LEFT, participant(user));
    }

    public void publishDiagramChanged(UUID diagramId) {
        publishDiagramChanged(diagramId, "actualizó el diagrama");
    }

    public void publishDiagramChanged(UUID diagramId, String action) {
        AppUser user = currentUserService.requireCurrentUser();
        requireMember(diagramId, user.getEmail());
        persistActivity(diagramId, user, action);
        publish(diagramId, DiagramEventType.DIAGRAM_CHANGED, participant(user), TextNode.valueOf(action));
    }

    public List<DiagramActivityResponse> findActivity(UUID diagramId) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        requireMember(diagramId, currentUser.getEmail());
        return activityEvents.findTop50ByDiagramIdOrderByCreatedAtDesc(diagramId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void publishEphemeralEvent(UUID diagramId, String email, DiagramEventType type, JsonNode payload) {
        if (type != DiagramEventType.DRAWING_PREVIEW && type != DiagramEventType.DRAWING_PREVIEW_CLEARED
                && type != DiagramEventType.ELEMENT_INTERACTION) {
            throw new IllegalArgumentException("El tipo de evento efímero no es válido.");
        }
        AppUser user = requireEditor(diagramId, email);
        validateEphemeralPayload(type, payload);
        publish(diagramId, type, participant(user), payload);
    }

    private AppUser requireMember(UUID diagramId, String email) {
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado."));
        requireProjectMembership(diagramId, user);
        return user;
    }

    private AppUser requireEditor(UUID diagramId, String email) {
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado."));
        Project project = requireProjectMembership(diagramId, user);
        if (user.getId().equals(project.getOwnerId())) {
            return user;
        }
        ProjectRole role = members.findByProjectIdAndUserId(project.getId(), user.getId())
                .map(ProjectMember::getRole)
                .orElseThrow(() -> new AccessDeniedException("No tienes acceso a este diagrama."));
        if (role != ProjectRole.OWNER && role != ProjectRole.EDITOR) {
            throw new AccessDeniedException("No tienes permiso para editar este diagrama.");
        }
        return user;
    }

    private Project requireProjectMembership(UUID diagramId, AppUser user) {
        Diagram diagram = diagrams.findById(diagramId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagrama", diagramId));
        Project project = projects.findById(diagram.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", diagram.getProjectId()));
        if (!user.getId().equals(project.getOwnerId())
                && members.findByProjectIdAndUserId(project.getId(), user.getId()).isEmpty()) {
            throw new AccessDeniedException("No tienes acceso a este diagrama.");
        }
        return project;
    }

    private void publish(UUID diagramId, DiagramEventType type, CollaborationParticipant participant) {
        publish(diagramId, type, participant, null);
    }

    private void publish(UUID diagramId, DiagramEventType type, CollaborationParticipant participant, JsonNode payload) {
        DiagramEvent event = new DiagramEvent(diagramId, type, payload, participant);
        messagingTemplate.convertAndSend("/topic/diagrams/" + diagramId, event);
        log.info("Evento de colaboración {} publicado para diagrama {} por usuario {}", type, diagramId,
                participant.userId());
    }

    private CollaborationParticipant participant(AppUser user) {
        return new CollaborationParticipant(user.getId(), user.getName());
    }

    private void persistActivity(UUID diagramId, AppUser actor, String action) {
        DiagramActivityEvent event = new DiagramActivityEvent();
        event.setDiagramId(diagramId);
        event.setActorId(actor.getId());
        event.setActorName(actor.getName());
        event.setAction(action);
        activityEvents.save(event);
    }

    private DiagramActivityResponse toResponse(DiagramActivityEvent event) {
        return new DiagramActivityResponse(event.getId(), event.getActorId(), event.getActorName(),
                event.getAction(), event.getCreatedAt());
    }

    private void validateEphemeralPayload(DiagramEventType type, JsonNode payload) {
        if (type == DiagramEventType.DRAWING_PREVIEW_CLEARED) {
            if (payload != null && !payload.isNull()) throw new IllegalArgumentException("Este evento no admite contenido.");
            return;
        }
        if (payload == null || !payload.isObject()) throw new IllegalArgumentException("El contenido del evento no es válido.");
        if (type == DiagramEventType.DRAWING_PREVIEW) {
            String svgPath = payload.path("svgPath").asText();
            if (svgPath.isBlank() || svgPath.length() > 12_000) {
                throw new IllegalArgumentException("La previsualización del trazo no es válida.");
            }
            return;
        }
        String elementId = payload.path("elementId").asText();
        String elementType = payload.path("elementType").asText();
        String state = payload.path("state").asText();
        try {
            UUID.fromString(elementId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El elemento seleccionado no es válido.");
        }
        if (!"CLASS".equals(elementType) || !Set.of("DRAGGING", "IDLE").contains(state)) {
            throw new IllegalArgumentException("La interacción del elemento no es válida.");
        }
    }
}
