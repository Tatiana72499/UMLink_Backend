package com.examensw1.umlcollab.features.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.service.CurrentUserService;
import com.examensw1.umlcollab.features.project.dto.CreateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectRequest;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository repository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ProjectService service;

    @Test
    void debeAsignarElCreadorAutenticadoAlCrearProyecto() {
        AppUser user = user("Tatiana");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateProjectRequest("Biblioteca", "Modelo UML"));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(repository).save(captor.capture());
        assertEquals(user.getId(), captor.getValue().getOwnerId());
        assertEquals(user.getName(), captor.getValue().getOwnerName());
    }

    @Test
    void debeOcultarProyectoQueNoPerteneceAlUsuarioAutenticado() {
        AppUser owner = user("Tatiana");
        Project project = new Project();
        project.setOwnerId(UUID.randomUUID());
        UUID projectId = UUID.randomUUID();
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(ResourceNotFoundException.class, () -> service.findById(projectId));
    }

    @Test
    void debeActualizarProyectoConLaVersionActual() {
        AppUser owner = user("Tatiana");
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        project.setOwnerId(owner.getId());
        project.setVersion(0L);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        when(repository.saveAndFlush(project)).thenReturn(project);

        service.update(projectId, new UpdateProjectRequest("Biblioteca", "Modelo", 0L));

        assertEquals("Biblioteca", project.getName());
        verify(repository).saveAndFlush(project);
    }

    @Test
    void debeRechazarActualizacionDeProyectoConVersionDesactualizada() {
        AppUser owner = user("Tatiana");
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setOwnerId(owner.getId());
        project.setVersion(2L);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(VersionConflictException.class,
                () -> service.update(projectId, new UpdateProjectRequest("Biblioteca", "Modelo", 1L)));

        verify(repository, never()).saveAndFlush(any());
    }

    private AppUser user(String name) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName(name);
        return user;
    }
}
