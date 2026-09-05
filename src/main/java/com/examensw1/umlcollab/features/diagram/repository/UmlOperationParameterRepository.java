package com.examensw1.umlcollab.features.diagram.repository;

import com.examensw1.umlcollab.features.diagram.model.UmlOperationParameter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UmlOperationParameterRepository extends JpaRepository<UmlOperationParameter, UUID> {
    List<UmlOperationParameter> findByUmlOperationIdOrderByParameterOrderAsc(UUID umlOperationId);
    void deleteByUmlOperationId(UUID umlOperationId);
}
