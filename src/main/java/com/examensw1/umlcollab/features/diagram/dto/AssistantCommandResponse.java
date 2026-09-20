package com.examensw1.umlcollab.features.diagram.dto;

public record AssistantCommandResponse(String action, String summary, boolean requiresConfirmation) {}