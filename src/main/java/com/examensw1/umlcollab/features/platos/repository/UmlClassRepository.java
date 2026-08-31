package com.examensw1.umlcollab.features.platos.repository;
import com.examensw1.umlcollab.features.platos.model.UmlClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UmlClassRepository extends JpaRepository<UmlClass, UUID> { List<UmlClass> findByDiagramId(UUID diagramId); }
