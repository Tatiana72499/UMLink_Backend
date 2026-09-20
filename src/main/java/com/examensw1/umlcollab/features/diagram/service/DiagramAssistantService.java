package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.features.diagram.dto.AssistantCommandResponse;
import com.examensw1.umlcollab.features.diagram.dto.CreateAttributeRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateUmlClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.ExecuteAssistantCommandRequest;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.model.AttributeDataType;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiagramAssistantService {
    private static final Pattern ATTRIBUTE = Pattern.compile("(?i)(?:agrega|agregar|añade|añadir)\\s+a\\s+([\\p{L}\\p{N}_ ]+?)\\s+un\\s+atributo\\s+(?:llamado|llamada|nombre)?\\s*([\\p{L}_][\\p{L}\\p{N}_]*)?(?:\\s*(?:de tipo|:)\\s*([A-Za-z]+))?$");
    private static final Pattern INHERITANCE = Pattern.compile("(?i)^\\s*([\\p{L}\\p{N}_ ]+?)\\s+(?:hereda|extiende)\\s+de\\s+([\\p{L}\\p{N}_ ]+)\\s*$");
    private static final Pattern RELATION = Pattern.compile("(?i)(?:crea|crear|agrega|agregar).*(?:relaci[oó]n|asociaci[oó]n).*(?:entre)\\s+([\\p{L}\\p{N}_ ]+?)\\s+y\\s+([\\p{L}\\p{N}_ ]+?)(?:,|$)");

    private final DiagramService diagramService;

    @Transactional
    public AssistantCommandResponse execute(UUID diagramId, ExecuteAssistantCommandRequest request) {
        DiagramDetailsResponse details = diagramService.getDetails(diagramId);
        Plan plan = plan(details, request.command());
        if (!request.confirmed()) return new AssistantCommandResponse(plan.action(), plan.summary(), true);
        switch (plan.action()) {
            case "CREATE_CLASS" -> diagramService.createClass(diagramId, new CreateUmlClassRequest(plan.first(), 120 + details.classes().size() * 35d, 120 + details.classes().size() * 25d, null));
            case "CREATE_ATTRIBUTE" -> diagramService.createAttribute(findClass(details, plan.first()).id(), new CreateAttributeRequest(plan.second(), dataType(plan.third()), "PRIVATE", "id".equalsIgnoreCase(plan.second())));
            case "CREATE_RELATION", "CREATE_INHERITANCE" -> diagramService.createRelation(diagramId, new CreateRelationRequest(findClass(details, plan.first()).id(), findClass(details, plan.second()).id(), "CREATE_INHERITANCE".equals(plan.action()) ? RelationType.GENERALIZATION : RelationType.ASSOCIATION, null, "1..1", "1..*"));
            default -> throw new IllegalArgumentException("El asistente no reconoce esa acción.");
        }
        return new AssistantCommandResponse(plan.action(), plan.summary(), false);
    }

    private Plan plan(DiagramDetailsResponse details, String command) {
        String text = command.trim();
        Matcher attribute = ATTRIBUTE.matcher(text);
        if (attribute.matches() && attribute.group(2) != null) return new Plan("CREATE_ATTRIBUTE", "Se agregará el atributo “" + attribute.group(2) + "” a “" + clean(attribute.group(1)) + "”.", clean(attribute.group(1)), attribute.group(2), attribute.group(3));
        Matcher inheritance = INHERITANCE.matcher(text);
        if (inheritance.matches()) return new Plan("CREATE_INHERITANCE", "Se creará la herencia de “" + clean(inheritance.group(1)) + "” hacia “" + clean(inheritance.group(2)) + "”.", clean(inheritance.group(1)), clean(inheritance.group(2)), null);
        Matcher relation = RELATION.matcher(text);
        if (relation.find()) return new Plan("CREATE_RELATION", "Se creará una asociación entre “" + clean(relation.group(1)) + "” y “" + clean(relation.group(2)) + "”.", clean(relation.group(1)), clean(relation.group(2)), null);
        String name = text.replaceFirst("(?i)^\\s*(crea|crear|agrega|agregar|nueva|nuevo)\\s+(?:una\\s+)?clase\\s*(?:llamada|llamado|nombre)?\\s*", "").trim();
        if (!name.isBlank() && !name.equals(text)) return new Plan("CREATE_CLASS", "Se creará la clase “" + clean(name) + "”.", clean(name), null, null);
        throw new IllegalArgumentException("No pude interpretar el comando. Prueba: “crear clase Pago”, “agrega a Usuario un atributo llamado telefono” o “Cliente hereda de Persona”.");
    }

    private UmlClassResponse findClass(DiagramDetailsResponse details, String name) { return details.classes().stream().filter(item -> item.name().equalsIgnoreCase(name.trim())).findFirst().orElseThrow(() -> new IllegalArgumentException("No existe la clase “" + name + "” en este diagrama.")); }
    private AttributeDataType dataType(String value) { if (value == null || value.isBlank()) return AttributeDataType.STRING; String normalized = value.replace(" ", "").toUpperCase(Locale.ROOT); return switch (normalized) { case "INT", "INTEGER", "ENTERO" -> AttributeDataType.INTEGER; case "LONG" -> AttributeDataType.LONG; case "DOUBLE", "DECIMAL" -> AttributeDataType.DOUBLE; case "BOOLEAN", "BOOL", "BOOLEANO" -> AttributeDataType.BOOLEAN; case "UUID" -> AttributeDataType.UUID; case "DATE", "LOCALDATE", "FECHA" -> AttributeDataType.LOCAL_DATE; case "DATETIME", "LOCALDATETIME", "FECHAHORA" -> AttributeDataType.LOCAL_DATE_TIME; default -> AttributeDataType.STRING; }; }
    private String clean(String value) { return value.trim().replaceAll("\\s+", " "); }
    private record Plan(String action, String summary, String first, String second, String third) {}
}