package com.examensw1.umlcollab.features.project.controller;

import com.examensw1.umlcollab.features.project.dto.CreateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.ProjectResponse;
import com.examensw1.umlcollab.features.project.dto.UpdateProjectRequest;
import com.examensw1.umlcollab.features.project.dto.*;
import com.examensw1.umlcollab.features.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController @Validated @RequestMapping("/api/projects") @RequiredArgsConstructor
public class ProjectController {
    private final ProjectService service;
    @GetMapping public List<ProjectResponse> findAll() { return service.findAll(); }
    @GetMapping("/{id}") public ProjectResponse findById(@PathVariable UUID id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) { return service.create(request); }
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @RequestParam @jakarta.validation.constraints.Min(0) Long version) { service.delete(id, version); }
    @GetMapping("/{id}/members") public List<ProjectMemberResponse> members(@PathVariable UUID id) { return service.members(id); }
    @PostMapping("/{id}/members") @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(@PathVariable UUID id, @Valid @RequestBody AddProjectMemberRequest request) { return service.addMember(id, request); }
    @PutMapping("/{id}/members/{memberId}") public ProjectMemberResponse updateMember(@PathVariable UUID id, @PathVariable UUID memberId, @Valid @RequestBody UpdateProjectMemberRequest request) { return service.updateMember(id, memberId, request); }
    @DeleteMapping("/{id}/members/{memberId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID id, @PathVariable UUID memberId) { service.removeMember(id, memberId); }
}
