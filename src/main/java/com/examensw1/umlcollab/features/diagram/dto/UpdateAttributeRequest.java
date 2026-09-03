package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import com.examensw1.umlcollab.features.diagram.model.AttributeDataType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAttributeRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull AttributeDataType dataType,
        @NotBlank @Size(max = 20) String visibility) {}
