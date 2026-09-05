package com.examensw1.umlcollab.features.project.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.repository.AppUserRepository;
import com.examensw1.umlcollab.features.auth.service.CurrentUserService;
import com.examensw1.umlcollab.features.project.dto.AddProjectMemberRequest;
import com.examensw1.umlcollab.features.project.dto.CreateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.ProjectMemberResponse;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectMemberRequest;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectRequest;
import com.examensw1.umlcollab.features.project.model.ProjectMember;
import com.examensw1.umlcollab.features.project.model.ProjectRole;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectMemberRepository;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Slf4j @RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository repository;
    private final CurrentUserService currentUserService;
    private final ProjectMemberRepository members;
    private final AppUserRepository users;

    public List<ProjectResponse> findAll() {
        AppUser currentUser = currentUserService.requireCurrentUser();
        return members.findByUserId(currentUser.getId()).stream().map(ProjectMember::getProjectId).distinct()
                .map(this::findWithoutAuthorization).map(this::toResponse).toList();
    }
    public ProjectResponse findById(UUID id) { return toResponse(findEntity(id)); }
    public Project findEntity(UUID id) {
        Project project = findWithoutAuthorization(id);
        requireRole(project, currentUserService.requireCurrentUser(), ProjectRole.VIEWER, ProjectRole.EDITOR, ProjectRole.OWNER);
        return project;
    }
    public Project findEditableEntity(UUID id) {
        Project project = findWithoutAuthorization(id);
        requireRole(project, currentUserService.requireCurrentUser(), ProjectRole.EDITOR, ProjectRole.OWNER);
        return project;
    }

    public Project findOwnedEntity(UUID id) {
        Project project = findWithoutAuthorization(id);
        requireRole(project, currentUserService.requireCurrentUser(), ProjectRole.OWNER);
        return project;
    }
    public ProjectResponse create(CreateProjectRequest request) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        Project project = new Project();
        project.setName(request.name()); project.setDescription(request.description());
        project.setOwnerId(currentUser.getId()); project.setOwnerName(currentUser.getName());
        Project saved = repository.save(project);
        ProjectMember owner = new ProjectMember();
        owner.setProjectId(saved.getId());
        owner.setUserId(currentUser.getId());
        owner.setRole(ProjectRole.OWNER);
        members.save(owner);
        log.info("Proyecto creado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = findOwnedEntity(id);
        verifyExpectedVersion("El proyecto", project.getVersion(), request.version());
        project.setName(request.name());
        project.setDescription(request.description());
        Project saved = repository.saveAndFlush(project);
        log.info("Proyecto actualizado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional
    public void delete(UUID id, Long version) {
        Project project = findOwnedEntity(id);
        verifyExpectedVersion("El proyecto", project.getVersion(), version);
        repository.delete(project);
        log.info("Proyecto eliminado: {}", id);
    }
    private void verifyExpectedVersion(String resource, Long actualVersion, Long expectedVersion) {
        if (!expectedVersion.equals(actualVersion)) throw new VersionConflictException(resource);
    }
    public List<ProjectMemberResponse> members(UUID projectId) {
        Project project = findEntity(projectId);
        return members.findByProjectId(projectId).stream()
                .map(member -> toMemberResponse(member, project.getOwnerId()))
                .toList();
    }
    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, AddProjectMemberRequest request) {
        Project project = findOwnedEntity(projectId);
        if (request.role() == ProjectRole.OWNER) throw new IllegalArgumentException("No se puede invitar otro propietario.");
        AppUser user = users.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No existe una cuenta con ese correo."));
        if (user.getId().equals(project.getOwnerId())) {
            log.warn("Intento de invitar al propietario del proyecto: projectId={}, userId={}", projectId, user.getId());
            throw new IllegalArgumentException("No puedes agregarte como colaborador porque ya eres la propietaria del proyecto.");
        }
        ProjectMember member = members.findByProjectIdAndUserId(projectId, user.getId()).orElseGet(ProjectMember::new);
        member.setProjectId(projectId); member.setUserId(user.getId()); member.setRole(request.role());
        return toMemberResponse(members.save(member));
    }
    @Transactional
    public ProjectMemberResponse updateMember(UUID projectId, UUID memberId, UpdateProjectMemberRequest request) {
        findOwnedEntity(projectId);
        ProjectMember member = members.findById(memberId)
                .filter(item -> item.getProjectId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Miembro", memberId));
        if (member.getRole() == ProjectRole.OWNER || request.role() == ProjectRole.OWNER) throw new IllegalArgumentException("El rol de propietario no se puede modificar.");
        member.setRole(request.role());
        return toMemberResponse(members.save(member));
    }

    @Transactional
    public void removeMember(UUID projectId, UUID memberId) {
        findOwnedEntity(projectId);
        ProjectMember member = members.findById(memberId)
                .filter(item -> item.getProjectId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Miembro", memberId));
        if (member.getRole() == ProjectRole.OWNER) throw new IllegalArgumentException("No se puede eliminar al propietario.");
        members.delete(member);
    }
    private Project findWithoutAuthorization(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proyecto", id));
    }

    private void requireRole(Project project, AppUser user, ProjectRole... allowed) {
        if (user.getId().equals(project.getOwnerId())) {
            return;
        }
        ProjectRole role = members.findByProjectIdAndUserId(project.getId(), user.getId())
                .map(ProjectMember::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", project.getId()));
        if (java.util.Arrays.stream(allowed).noneMatch(role::equals)) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos para modificar este proyecto.");
        }
    }
    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return toMemberResponse(member, null);
    }
    private ProjectMemberResponse toMemberResponse(ProjectMember member, UUID ownerId) {
        AppUser user = users.findById(member.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Usuario", member.getUserId()));
        ProjectRole role = member.getUserId().equals(ownerId) ? ProjectRole.OWNER : member.getRole();
        return new ProjectMemberResponse(member.getId(), user.getId(), user.getName(), user.getEmail(), role);
    }
    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getOwnerName(),
                project.getVersion(), project.getCreatedAt());
    }
}
