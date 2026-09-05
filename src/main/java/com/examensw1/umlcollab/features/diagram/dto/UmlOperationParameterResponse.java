package com.examensw1.umlcollab.features.diagram.dto;

import java.util.UUID;

public record UmlOperationParameterResponse(UUID id, String name, String dataType, int parameterOrder) {}
