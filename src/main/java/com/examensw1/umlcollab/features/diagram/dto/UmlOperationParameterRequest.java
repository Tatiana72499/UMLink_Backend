package com.examensw1.umlcollab.features.diagram.dto;

import com.examensw1.umlcollab.features.diagram.model.AttributeDataType;
import jakarta.validation.constraints.*;

public record UmlOperationParameterRequest(@NotBlank @Size(max = 120) String name, @NotNull AttributeDataType dataType) {}
