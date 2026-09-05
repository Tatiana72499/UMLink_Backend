package com.examensw1.umlcollab.features.collaboration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.repository.AppUserRepository;
import com.examensw1.umlcollab.features.auth.service.CurrentUserService;
import com.examensw1.umlcollab.features.collaboration.model.DiagramActivityEvent;
import com.examensw1.umlcollab.features.collaboration.repository.DiagramActivityEventRepository;
import com.examensw1.umlcollab.features.collaboration.dto.DiagramEvent;
import com.examensw1.umlcollab.features.diagram.model.Diagram;
import com.examensw1.umlcollab.features.diagram.repository.DiagramRepository;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectMemberRepository;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class CollaborationServiceTest {

    @Mock private AppUserRepository users;
    @Mock private DiagramRepository diagrams;
    @Mock private ProjectRepository projects;
    @Mock private ProjectMemberRepository members;
    @Mock private CurrentUserService currentUserService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private DiagramActivityEventRepository activityEvents;
    @InjectMocks private CollaborationService service;

    @Test
    void debePersistirActividadAntesDeDifundirCambioDeDiagrama() {
        UUID diagramId = UUID.randomUUID();
        AppUser owner = new AppUser();
        owner.setId(UUID.randomUUID());
        owner.setName("Tatiana");
        owner.setEmail("tatiana@umlink.dev");
        Diagram diagram = new Diagram();
        diagram.setId(diagramId);
        UUID projectId = UUID.randomUUID();
        diagram.setProjectId(projectId);
        Project project = new Project();
        project.setId(projectId);
        project.setOwnerId(owner.getId());
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(diagrams.findById(diagramId)).thenReturn(Optional.of(diagram));
        when(projects.findById(projectId)).thenReturn(Optional.of(project));

        service.publishDiagramChanged(diagramId, "creó una clase");

        ArgumentCaptor<DiagramActivityEvent> captor = ArgumentCaptor.forClass(DiagramActivityEvent.class);
        verify(activityEvents).save(captor.capture());
        assertEquals(diagramId, captor.getValue().getDiagramId());
        assertEquals(owner.getId(), captor.getValue().getActorId());
        assertEquals("creó una clase", captor.getValue().getAction());
        verify(messagingTemplate).convertAndSend(any(String.class), any(DiagramEvent.class));
    }
}
