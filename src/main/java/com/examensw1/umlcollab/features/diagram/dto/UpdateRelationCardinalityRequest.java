package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRelationCardinalityRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = "(1\\.\\.1|0\\.\\.1|1\\.\\.\\*)") String sourceCardinality,
        @NotBlank @Size(max = 20) @Pattern(regexp = "(1\\.\\.1|0\\.\\.1|1\\.\\.\\*)") String targetCardinality) {}
