package com.examensw1.umlcollab.features.project.repository;

import com.examensw1.umlcollab.features.project.model.ProjectMember;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectId(UUID projectId);
    List<ProjectMember> findByUserId(UUID userId);
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
