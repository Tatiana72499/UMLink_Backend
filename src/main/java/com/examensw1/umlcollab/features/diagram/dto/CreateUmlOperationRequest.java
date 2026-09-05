package com.examensw1.umlcollab.features.diagram.dto;

import com.examensw1.umlcollab.features.diagram.model.OperationReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateUmlOperationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE|PROTECTED") String visibility,
        @NotNull OperationReturnType returnType,
        @NotNull @Size(max = 10) List<@Valid UmlOperationParameterRequest> parameters) {}
