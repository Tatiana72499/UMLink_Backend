package com.examensw1.umlcollab.features.diagram.repository;

import com.examensw1.umlcollab.features.diagram.model.UmlOperation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UmlOperationRepository extends JpaRepository<UmlOperation, UUID> {
    List<UmlOperation> findByUmlClassId(UUID umlClassId);
}
