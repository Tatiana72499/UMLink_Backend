package com.examensw1.umlcollab.features.diagram.dto;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.util.UUID;
import java.util.List;
import jakarta.validation.Valid;
public record CreateRelationRequest(
        @NotNull UUID sourceClassId,
        @NotNull UUID targetClassId,
        @NotNull RelationType type,
        @Size(max = 120) String label,
        @Size(max = 20) @Pattern(regexp = "(1\\.\\.1|0\\.\\.1|1\\.\\.\\*)") String sourceCardinality,
        @Size(max = 20) @Pattern(regexp = "(1\\.\\.1|0\\.\\.1|1\\.\\.\\*)") String targetCardinality,
        @DecimalMin("0.0") Double bendX,
        @DecimalMin("0.0") Double bendY,
        UUID associationClassId,
        @Size(max = 20) List<@Valid RelationAlignmentPoint> alignmentPoints) {
    public CreateRelationRequest(UUID sourceClassId, UUID targetClassId, RelationType type, String label, String sourceCardinality, String targetCardinality) {
        this(sourceClassId, targetClassId, type, label, sourceCardinality, targetCardinality, null, null, null, null);
    }
    public CreateRelationRequest(UUID sourceClassId, UUID targetClassId, RelationType type, String label, String sourceCardinality, String targetCardinality, Double bendX, Double bendY, UUID associationClassId) {
        this(sourceClassId, targetClassId, type, label, sourceCardinality, targetCardinality, bendX, bendY, associationClassId, null);
    }
}
