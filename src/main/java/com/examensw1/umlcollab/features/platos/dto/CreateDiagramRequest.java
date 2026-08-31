package com.examensw1.umlcollab.features.platos.dto;
import jakarta.validation.constraints.NotBlank;
public record CreateDiagramRequest(@NotBlank String name) {}
