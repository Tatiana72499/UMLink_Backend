package com.examensw1.umlcollab.features.diagram.dto;
import java.util.UUID;
public record UmlAttributeResponse(UUID id, UUID umlClassId, String name, String dataType, String visibility, boolean primaryKey, int attributeOrder) {
    public UmlAttributeResponse(UUID id, UUID umlClassId, String name, String dataType, String visibility, boolean primaryKey) {
        this(id, umlClassId, name, dataType, visibility, primaryKey, 0);
    }
}
