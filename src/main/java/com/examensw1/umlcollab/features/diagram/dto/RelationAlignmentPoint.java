package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.DecimalMin;

public record RelationAlignmentPoint(@DecimalMin("0.0") double x, @DecimalMin("0.0") double y) {}
