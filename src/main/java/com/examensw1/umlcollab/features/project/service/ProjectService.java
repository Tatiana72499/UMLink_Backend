package com.examensw1.umlcollab.features.project.service;

import com.examensw1.umlcollab.common.exception.ResourceNotFoundException;
import com.examensw1.umlcollab.features.project.dto.CreateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.model.Project;
import com.examensw1.umlcollab.features.project.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service @Slf4j @RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository repository;

    public List<ProjectResponse> findAll() { return repository.findAll().stream().map(this::toResponse).toList(); }
    public ProjectResponse findById(UUID id) { return toResponse(findEntity(id)); }
    public Project findEntity(UUID id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proyecto", id)); }
    public ProjectResponse create(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name()); project.setDescription(request.description()); project.setOwnerName(request.ownerName());
        Project saved = repository.save(project);
        log.info("Proyecto creado: {}", saved.getId());
        return toResponse(saved);
    }
    private ProjectResponse toResponse(Project project) { return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), project.getOwnerName(), project.getCreatedAt()); }
}
