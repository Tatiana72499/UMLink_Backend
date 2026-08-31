package com.examensw1.umlcollab.features.diagram.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateAttributeRequest(@NotBlank String name, @NotBlank String dataType, String visibility) {}
