package com.examensw1.umlcollab.features.platos.repository;
import com.examensw1.umlcollab.features.platos.model.Diagram;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DiagramRepository extends JpaRepository<Diagram, UUID> { List<Diagram> findByProjectId(UUID projectId); }
