package com.examensw1.umlcollab.features.collaboration.repository;

import com.examensw1.umlcollab.features.collaboration.model.DiagramActivityEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagramActivityEventRepository extends JpaRepository<DiagramActivityEvent, UUID> {

    List<DiagramActivityEvent> findTop50ByDiagramIdOrderByCreatedAtDesc(UUID diagramId);
}
