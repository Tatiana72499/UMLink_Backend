package com.examensw1.umlcollab.features.diagram.dto;
import jakarta.validation.constraints.NotBlank;
public record UpdateUmlClassRequest(@NotBlank String name, double positionX, double positionY) {}
