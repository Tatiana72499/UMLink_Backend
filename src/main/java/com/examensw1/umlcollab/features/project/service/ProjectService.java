package com.examensw1.umlcollab.features.project.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.common.exception.VersionConflictException;
import com.examensw1.umlcollab.features.auth.model.AppUser;
import com.examensw1.umlcollab.features.auth.service.CurrentUserService;
import com.examensw1.umlcollab.features.project.dto.CreateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectRequest;
import com.examensw1.umlcollab.features.project.model.Project;
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

    public List<ProjectResponse> findAll() {
        AppUser currentUser = currentUserService.requireCurrentUser();
        return repository.findByOwnerId(currentUser.getId()).stream().map(this::toResponse).toList();
    }
    public ProjectResponse findById(UUID id) { return toResponse(findEntity(id)); }
    public Project findEntity(UUID id) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        Project project = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proyecto", id));
        if (!currentUser.getId().equals(project.getOwnerId())) {
            throw new ResourceNotFoundException("Proyecto", id);
        }
        return project;
    }
    public ProjectResponse create(CreateProjectRequest request) {
        AppUser currentUser = currentUserService.requireCurrentUser();
        Project project = new Project();
        project.setName(request.name()); project.setDescription(request.description());
        project.setOwnerId(currentUser.getId()); project.setOwnerName(currentUser.getName());
        Project saved = repository.save(project);
        log.info("Proyecto creado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = findEntity(id);
        verifyExpectedVersion("El proyecto", project.getVersion(), request.version());
        project.setName(request.name());
        project.setDescription(request.description());
        Project saved = repository.saveAndFlush(project);
        log.info("Proyecto actualizado: {}", saved.getId());
        return toResponse(saved);
    }
    @Transactional
    public void delete(UUID id, Long version) {
        Project project = findEntity(id);
        verifyExpectedVersion("El proyecto", project.getVersion(), version);
        repository.delete(project);
        log.info("Proyecto eliminado: {}", id);
    }
    private void verifyExpectedVersion(String resource, Long actualVersion, Long expectedVersion) {
        if (!expectedVersion.equals(actualVersion)) throw new VersionConflictException(resource);
    }
    private ProjectResponse toResponse(Project project) { return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getOwnerName(), project.getVersion(), project.getCreatedAt()); }
}
