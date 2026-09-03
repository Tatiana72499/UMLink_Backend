package com.examensw1.umlcollab.features.diagram.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record UpdateUmlClassRequest(@NotBlank String name, double positionX, double positionY, @Pattern(regexp = "#[0-9A-Fa-f]{6}") String fillColor) {}
