package com.examensw1.umlcollab.features.platos.dto;
import com.examensw1.umlcollab.features.platos.model.RelationType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateRelationRequest(@NotNull UUID sourceClassId, @NotNull UUID targetClassId, @NotNull RelationType type) {}
