package com.examensw1.umlcollab.features.diagram.repository;

import com.examensw1.umlcollab.features.diagram.model.DiagramDrawing;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagramDrawingRepository extends JpaRepository<DiagramDrawing, UUID> {

    List<DiagramDrawing> findByDiagramIdOrderByCreatedAtAsc(UUID diagramId);

    void deleteByDiagramId(UUID diagramId);
}
