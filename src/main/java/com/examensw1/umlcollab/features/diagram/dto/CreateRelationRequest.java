package com.examensw1.umlcollab.features.diagram.dto;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateRelationRequest(@NotNull UUID sourceClassId, @NotNull UUID targetClassId, @NotNull RelationType type) {}
