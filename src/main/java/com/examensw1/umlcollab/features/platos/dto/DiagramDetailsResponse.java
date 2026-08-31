package com.examensw1.umlcollab.features.platos.dto;
import java.util.List;
public record DiagramDetailsResponse(DiagramResponse diagram, List<UmlClassResponse> classes, List<UmlRelationResponse> relations) {}
