package com.examensw1.umlcollab.features.diagram.dto;
import com.examensw1.umlcollab.features.diagram.model.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record CreateAttributeRequest(@NotBlank String name, @NotNull AttributeDataType dataType, String visibility) {}
