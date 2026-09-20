package com.examensw1.umlcollab.features.diagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExecuteAssistantCommandRequest(@NotBlank @Size(max = 500) String command, boolean confirmed) {}