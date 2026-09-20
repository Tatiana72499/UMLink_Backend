package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** Complete order for the attributes that belong to one UML class. */
public record UpdateAttributeOrderRequest(@NotEmpty List<UUID> attributeIds) {}
