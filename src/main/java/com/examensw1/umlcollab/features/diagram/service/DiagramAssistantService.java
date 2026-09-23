package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.features.diagram.dto.AssistantCommandResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.ExecuteAssistantCommandRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiagramAssistantService {
    private final DiagramService diagramService;
    private final DiagramAssistantPlanner planner;

    @Transactional
    public AssistantCommandResponse execute(UUID diagramId, ExecuteAssistantCommandRequest request) {
        DiagramDetailsResponse details = diagramService.getDetails(diagramId);
        DiagramAssistantPlanner.Plan plan = planner.plan(diagramId, details, request.command());
        if (plan.readOnly()) return new AssistantCommandResponse(plan.action(), plan.summary(), false);
        if (!request.confirmed()) return new AssistantCommandResponse(plan.action(), plan.summary(), true);
        plan.execute().run();
        return new AssistantCommandResponse(plan.action(), plan.summary(), false);
    }
}
