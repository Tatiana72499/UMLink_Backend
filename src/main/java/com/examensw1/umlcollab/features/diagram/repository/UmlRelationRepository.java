package com.examensw1.umlcollab.features.diagram.repository;
import com.examensw1.umlcollab.features.diagram.model.UmlRelation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UmlRelationRepository extends JpaRepository<UmlRelation, UUID> { List<UmlRelation> findByDiagramId(UUID diagramId); }
