package com.examensw1.umlcollab.features.diagram.dto;

import java.util.List;
import java.util.UUID;

public record UmlOperationResponse(UUID id, UUID umlClassId, String name, String visibility, String returnType, List<UmlOperationParameterResponse> parameters) {}
