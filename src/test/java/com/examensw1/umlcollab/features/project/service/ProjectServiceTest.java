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
import com.examensw1.umlcollab.features.project.dto.AddProjectMemberRequest;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectRequest;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import com.examensw1.umlcollab.features.project.repository.ProjectMemberRepository;
import com.examensw1.umlcollab.features.project.model.ProjectMember;
import com.examensw1.umlcollab.features.project.model.ProjectRole;
import java.util.Optional;
import java.util.List;
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
    @Mock private ProjectMemberRepository members;
    @Mock private com.examensw1.umlcollab.features.auth.repository.AppUserRepository users;

    @InjectMocks
    private ProjectService service;

    @Test
    void debeAsignarElCreadorAutenticadoAlCrearProyecto() {
        AppUser user = user("Tatiana");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(members.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        UUID projectId = UUID.randomUUID();
        project.setId(projectId);
        project.setOwnerId(UUID.randomUUID());
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(projectId, owner.getId())).thenReturn(Optional.empty());

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
        project.setId(projectId);
        project.setOwnerId(owner.getId());
        project.setVersion(2L);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));

        assertThrows(VersionConflictException.class,
                () -> service.update(projectId, new UpdateProjectRequest("Biblioteca", "Modelo", 1L)));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void debePermitirQueUnEditorConsulteElProyecto() {
        AppUser editor = user("Editora");
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId);
        when(currentUserService.requireCurrentUser()).thenReturn(editor);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        when(members.findByProjectIdAndUserId(projectId, editor.getId()))
                .thenReturn(Optional.of(member(projectId, editor.getId(), ProjectRole.EDITOR)));

        service.findById(projectId);

        verify(repository).findById(projectId);
    }

    @Test
    void debeImpedirQueViewerInviteMiembros() {
        AppUser viewer = user("Lectora");
        UUID projectId = UUID.randomUUID();
        when(currentUserService.requireCurrentUser()).thenReturn(viewer);
        when(repository.findById(projectId)).thenReturn(Optional.of(project(projectId)));
        when(members.findByProjectIdAndUserId(projectId, viewer.getId()))
                .thenReturn(Optional.of(member(projectId, viewer.getId(), ProjectRole.VIEWER)));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.addMember(projectId, new AddProjectMemberRequest("otra@umlink.dev", ProjectRole.EDITOR)));

        verify(users, never()).findByEmail(any());
    }

    @Test
    void debeImpedirQueElPropietarioSeInviteComoColaborador() {
        AppUser owner = user("Tatiana");
        owner.setEmail("tatiana@umlink.dev");
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId);
        project.setOwnerId(owner.getId());
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addMember(projectId, new AddProjectMemberRequest(owner.getEmail(), ProjectRole.EDITOR)));

        assertEquals("No puedes agregarte como colaborador porque ya eres la propietaria del proyecto.", exception.getMessage());
        verify(members, never()).save(any(ProjectMember.class));
    }

    @Test
    void debeReconocerAlCreadorComoPropietarioAunqueSuMembresiaEsteDesactualizada() {
        AppUser owner = user("Tatiana");
        UUID projectId = UUID.randomUUID();
        Project project = project(projectId);
        project.setOwnerId(owner.getId());
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        when(members.findByProjectId(projectId))
                .thenReturn(List.of(member(projectId, owner.getId(), ProjectRole.EDITOR)));
        when(users.findById(owner.getId())).thenReturn(Optional.of(owner));

        assertEquals(ProjectRole.OWNER, service.members(projectId).getFirst().role());
    }

    private AppUser user(String name) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setName(name);
        return user;
    }
    private Project project(UUID projectId) {
        Project project = new Project();
        project.setId(projectId);
        return project;
    }

    private ProjectMember member(UUID projectId, UUID userId) {
        return member(projectId, userId, ProjectRole.OWNER);
    }

    private ProjectMember member(UUID projectId, UUID userId, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }
}
