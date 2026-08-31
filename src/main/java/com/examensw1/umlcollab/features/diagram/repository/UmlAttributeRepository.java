package com.examensw1.umlcollab.features.diagram.repository;
import com.examensw1.umlcollab.features.diagram.model.UmlAttribute;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UmlAttributeRepository extends JpaRepository<UmlAttribute, UUID> { List<UmlAttribute> findByUmlClassId(UUID umlClassId); }
