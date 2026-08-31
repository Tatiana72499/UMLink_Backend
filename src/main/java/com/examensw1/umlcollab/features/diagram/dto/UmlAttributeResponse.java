package com.examensw1.umlcollab.features.diagram.dto;
import java.util.UUID;
public record UmlAttributeResponse(UUID id, UUID umlClassId, String name, String dataType, String visibility) {}
