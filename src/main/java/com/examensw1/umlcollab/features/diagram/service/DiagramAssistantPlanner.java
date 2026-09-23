package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.features.diagram.dto.CreateAttributeRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateUmlClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.CreateUmlOperationRequest;
import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationParameterRequest;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UpdateAttributeRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateRelationCardinalityRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateRelationRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateUmlClassRequest;
import com.examensw1.umlcollab.features.diagram.dto.UpdateUmlOperationRequest;
import com.examensw1.umlcollab.features.diagram.model.AttributeDataType;
import com.examensw1.umlcollab.features.diagram.model.OperationReturnType;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Converts a bounded Spanish command into one existing, validated diagram operation.
 * The same command is planned again when confirmed, so no client-supplied IDs are trusted.
 */
@Component
public class DiagramAssistantPlanner {
    private final DiagramService diagramService;

    public DiagramAssistantPlanner(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    public Plan plan(UUID diagramId, DiagramDetailsResponse details, String command) {
        String text = command.trim().replaceAll("\\s+", " ");
        Plan result = read(details, text);
        if (result == null) result = classCommand(diagramId, details, text);
        if (result == null) result = attributeCommand(details, text);
        if (result == null) result = operationCommand(details, text);
        if (result == null) result = relationCommand(diagramId, details, text);
        if (result == null) {
            throw new IllegalArgumentException("No entendí la instrucción. Prueba «crear clase Pago», «mover clase Pago a 200, 300» o «eliminar relación entre Pago y Cliente».");
        }
        return result;
    }

    private Plan read(DiagramDetailsResponse details, String text) {
        Matcher m = match("^(?:lista|listar|muestra|mostrar|consulta|consultar) (?:las )?clases$", text);
        if (m != null) return info("READ_CLASSES", details.classes().isEmpty() ? "El diagrama no tiene clases." :
                "Clases: " + String.join(", ", details.classes().stream().map(UmlClassResponse::name).toList()) + ".");
        m = match("^(?:lista|listar|muestra|mostrar|consulta|consultar) (?:las )?relaciones$", text);
        if (m != null) return info("READ_RELATIONS", details.relations().isEmpty() ? "El diagrama no tiene relaciones." :
                "Relaciones: " + String.join("; ", details.relations().stream().map(relation ->
                        className(details, relation.sourceClassId()) + " → " + className(details, relation.targetClassId()) + " (" + relation.type() + ")").toList()) + ".");
        m = match("^(?:muestra|mostrar|consulta|consultar) (?:la )?clase (.+)$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            return info("READ_CLASS", item.name() + ": " + item.attributes().size() + " atributo(s), " + item.operations().size() + " operación(es).");
        }
        m = match("^(?:muestra|mostrar|lista|listar) (?:los )?atributos de (?:la clase )?(.+)$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            return info("READ_ATTRIBUTES", item.attributes().isEmpty() ? item.name() + " no tiene atributos." :
                    item.name() + ": " + String.join(", ", item.attributes().stream().map(attribute -> attribute.name() + ": " + attribute.dataType()).toList()) + ".");
        }
        m = match("^(?:muestra|mostrar|lista|listar) (?:las )?operaciones de (?:la clase )?(.+)$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            return info("READ_OPERATIONS", item.operations().isEmpty() ? item.name() + " no tiene operaciones." :
                    item.name() + ": " + String.join(", ", item.operations().stream().map(UmlOperationResponse::name).toList()) + ".");
        }
        return null;
    }

    private Plan classCommand(UUID diagramId, DiagramDetailsResponse details, String text) {
        Matcher m = match("^(?:mueve|mover) (?:la )?clase (.+?) a \\(?([0-9]+(?:\\.[0-9]+)?)\\s*[,;]\\s*([0-9]+(?:\\.[0-9]+)?)\\)?$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            double x = coordinate(m.group(2)), y = coordinate(m.group(3));
            return change("MOVE_CLASS", "Se moverá «" + item.name() + "» a (" + x + ", " + y + ").",
                    () -> diagramService.updateClass(item.id(), new UpdateUmlClassRequest(item.name(), x, y, item.fillColor())));
        }
        m = match("^(?:renombra|renombrar|edita|editar) (?:la )?clase (.+?) (?:a|como) (.+)$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            String newName = validName(m.group(2));
            return change("UPDATE_CLASS", "Se renombrará «" + item.name() + "» a «" + newName + "».",
                    () -> diagramService.updateClass(item.id(), new UpdateUmlClassRequest(newName, item.positionX(), item.positionY(), item.fillColor())));
        }
        m = match("^(?:elimina|eliminar|borra|borrar) (?:la )?clase (.+)$", text);
        if (m != null) {
            UmlClassResponse item = findClass(details, m.group(1));
            return change("DELETE_CLASS", "Se eliminará la clase «" + item.name() + "» y sus elementos asociados.",
                    () -> diagramService.deleteClass(item.id()));
        }
        m = match("^(?:crea|crear|agrega|agregar) (?:una )?clase (?:llamada |llamado )?(.+)$", text);
        if (m != null) {
            String name = validName(m.group(1));
            if (details.classes().stream().anyMatch(item -> item.name().equalsIgnoreCase(name)))
                throw new IllegalArgumentException("Ya existe una clase llamada «" + name + "».");
            double offset = details.classes().size() * 35d;
            return change("CREATE_CLASS", "Se creará la clase «" + name + "».",
                    () -> diagramService.createClass(diagramId, new CreateUmlClassRequest(name, 120 + offset, 120 + offset, null)));
        }
        return null;
    }

    private Plan attributeCommand(DiagramDetailsResponse details, String text) {
        Matcher m = match("^(?:agrega|agregar|añade|añadir|crea|crear) a (.+?) un atributo (?:llamado |llamada )?([\\p{L}_][\\p{L}\\p{N}_]*)(?: (?:de tipo|tipo) ([\\p{L}]+))?$", text);
        if (m != null) return createAttribute(details, m.group(1), m.group(2), m.group(3));
        m = match("^(?:agrega|agregar|crea|crear) (?:el )?atributo ([\\p{L}_][\\p{L}\\p{N}_]*) (?:a|en) (?:la )?clase (.+?)(?: (?:de tipo|tipo) ([\\p{L}]+))?$", text);
        if (m != null) return createAttribute(details, m.group(2), m.group(1), m.group(3));
        m = match("^(?:renombra|renombrar|edita|editar) (?:el )?atributo (\\S+) de (?:la )?clase (.+?) a (\\S+)$", text);
        if (m != null) {
            UmlAttributeResponse item = findAttribute(findClass(details, m.group(2)), m.group(1));
            String name = validName(m.group(3));
            return change("UPDATE_ATTRIBUTE", "Se renombrará el atributo «" + item.name() + "» a «" + name + "».",
                    () -> diagramService.updateAttribute(item.id(), new UpdateAttributeRequest(name, dataType(item.dataType()), item.visibility(), item.primaryKey())));
        }
        m = match("^(?:cambia|cambiar) (?:el )?tipo de (?:el )?atributo (\\S+) de (?:la )?clase (.+?) a ([\\p{L}]+)$", text);
        if (m != null) {
            UmlAttributeResponse item = findAttribute(findClass(details, m.group(2)), m.group(1));
            AttributeDataType type = dataType(m.group(3));
            return change("UPDATE_ATTRIBUTE", "Se cambiará el tipo de «" + item.name() + "» a " + type.displayName() + ".",
                    () -> diagramService.updateAttribute(item.id(), new UpdateAttributeRequest(item.name(), type, item.visibility(), item.primaryKey())));
        }
        m = match("^(?:elimina|eliminar|borra|borrar|quita|quitar) (?:el )?atributo (\\S+) (?:de|del|en) (?:la )?(?:clase |tabla )?(.+)$",
                text.replaceFirst("[.!?]+$", "").trim());
        if (m != null) {
            UmlAttributeResponse item = findAttribute(findClass(details, m.group(2)), m.group(1));
            return change("DELETE_ATTRIBUTE", "Se eliminará el atributo «" + item.name() + "».", () -> diagramService.deleteAttribute(item.id()));
        }
        return null;
    }

    private Plan createAttribute(DiagramDetailsResponse details, String className, String attributeName, String rawType) {
        UmlClassResponse owner = findClass(details, className);
        String name = validName(attributeName);
        if (owner.attributes().stream().anyMatch(item -> item.name().equalsIgnoreCase(name)))
            throw new IllegalArgumentException("La clase «" + owner.name() + "» ya tiene un atributo «" + name + "».");
        AttributeDataType type = rawType == null ? AttributeDataType.STRING : dataType(rawType);
        return change("CREATE_ATTRIBUTE", "Se agregará «" + name + ": " + type.displayName() + "» a «" + owner.name() + "».",
                () -> diagramService.createAttribute(owner.id(), new CreateAttributeRequest(name, type, "PRIVATE", "id".equalsIgnoreCase(name))));
    }

    private Plan operationCommand(DiagramDetailsResponse details, String text) {
        Matcher m = match("^(?:crea|crear|agrega|agregar) (?:una )?operaci[oó]n (\\S+) (?:a|en) (?:la )?clase (.+?)(?: (?:con retorno|que devuelve|devuelve) ([\\p{L}]+))?$", text);
        if (m != null) {
            UmlClassResponse owner = findClass(details, m.group(2));
            String name = validName(m.group(1));
            OperationReturnType type = m.group(3) == null ? OperationReturnType.VOID : returnType(m.group(3));
            if (owner.operations().stream().anyMatch(item -> item.name().equalsIgnoreCase(name)))
                throw new IllegalArgumentException("La clase ya tiene una operación «" + name + "».");
            return change("CREATE_OPERATION", "Se agregará la operación «" + name + "» a «" + owner.name() + "».",
                    () -> diagramService.createOperation(owner.id(), new CreateUmlOperationRequest(name, "PUBLIC", type, List.of())));
        }
        m = match("^(?:renombra|renombrar|edita|editar) (?:la )?operaci[oó]n (\\S+) de (?:la )?clase (.+?) a (\\S+)$", text);
        if (m != null) {
            UmlOperationResponse item = findOperation(findClass(details, m.group(2)), m.group(1));
            String name = validName(m.group(3));
            return change("UPDATE_OPERATION", "Se renombrará la operación «" + item.name() + "» a «" + name + "».",
                    () -> diagramService.updateOperation(item.id(), new UpdateUmlOperationRequest(name, item.visibility(), returnType(item.returnType()), parameters(item))));
        }
        m = match("^(?:cambia|cambiar) (?:el )?retorno de (?:la )?operaci[oó]n (\\S+) de (?:la )?clase (.+?) a ([\\p{L}]+)$", text);
        if (m != null) {
            UmlOperationResponse item = findOperation(findClass(details, m.group(2)), m.group(1));
            OperationReturnType type = returnType(m.group(3));
            return change("UPDATE_OPERATION", "Se cambiará el retorno de «" + item.name() + "» a " + type.displayName() + ".",
                    () -> diagramService.updateOperation(item.id(), new UpdateUmlOperationRequest(item.name(), item.visibility(), type, parameters(item))));
        }
        m = match("^(?:elimina|eliminar|borra|borrar) (?:la )?operaci[oó]n (\\S+) de (?:la )?clase (.+)$", text);
        if (m != null) {
            UmlOperationResponse item = findOperation(findClass(details, m.group(2)), m.group(1));
            return change("DELETE_OPERATION", "Se eliminará la operación «" + item.name() + "».", () -> diagramService.deleteOperation(item.id()));
        }
        return null;
    }

    private Plan relationCommand(UUID diagramId, DiagramDetailsResponse details, String text) {
        Matcher m = match("^(.+?) (?:hereda|extiende) de (.+)$", text);
        if (m != null) return createRelation(diagramId, details, m.group(1), m.group(2), RelationType.GENERALIZATION, null, null);
        m = match("^(?:crea|crear|agrega|agregar) (?:una )?(relaci[oó]n|asociaci[oó]n|agregaci[oó]n|composici[oó]n|herencia|generalizaci[oó]n|realizaci[oó]n|dependencia)(?: de tipo (\\S+))? entre (.+?) y (.+?)(?: con cardinalidades (\\S+) y (\\S+))?$", text);
        if (m != null) return createRelation(diagramId, details, m.group(3), m.group(4),
                relationType(m.group(2) == null ? m.group(1) : m.group(2)), m.group(5), m.group(6));
        m = match("^(?:elimina|eliminar|borra|borrar) (?:la )?(relaci[oó]n|asociaci[oó]n|agregaci[oó]n|composici[oó]n|herencia|generalizaci[oó]n|realizaci[oó]n|dependencia) entre (.+?) y (.+)$", text);
        if (m != null) {
            RelationType type = isGenericRelation(m.group(1)) ? null : relationType(m.group(1));
            UmlRelationResponse item = findRelation(details, m.group(2), m.group(3), type);
            return change("DELETE_RELATION", "Se eliminará la relación " + item.type() + " entre «" + className(details, item.sourceClassId()) + "» y «" + className(details, item.targetClassId()) + "».",
                    () -> diagramService.deleteRelation(item.id()));
        }
        m = match("^(?:cambia|cambiar) (?:el )?tipo de (?:la )?relaci[oó]n entre (.+?) y (.+?) a (\\S+)$", text);
        if (m != null) {
            UmlRelationResponse item = findRelation(details, m.group(1), m.group(2), null);
            RelationType type = relationType(m.group(3));
            boolean cardinality = usesCardinality(type);
            return change("UPDATE_RELATION", "Se cambiará la relación a " + type + ".",
                    () -> diagramService.updateRelation(item.id(), new UpdateRelationRequest(item.sourceClassId(), item.targetClassId(), type,
                            supportsLabel(type) ? item.label() : null, cardinality ? defaultCardinality(item.sourceCardinality()) : null,
                            cardinality ? defaultCardinality(item.targetCardinality()) : null, item.bendX(), item.bendY(),
                            type == RelationType.ASSOCIATION ? item.associationClassId() : null, item.alignmentPoints())));
        }
        m = match("^(?:cambia|cambiar) (?:las )?cardinalidades de (?:la )?relaci[oó]n entre (.+?) y (.+?) a (\\S+) y (\\S+)$", text);
        if (m != null) {
            UmlRelationResponse item = findRelation(details, m.group(1), m.group(2), null);
            String source = cardinality(m.group(3)), target = cardinality(m.group(4));
            if (!item.sourceClassId().equals(findClass(details, m.group(1)).id())) {
                String swap = source;
                source = target;
                target = swap;
            }
            String sourceCardinality = source, targetCardinality = target;
            return change("UPDATE_RELATION", "Se cambiarán las cardinalidades a " + source + " y " + target + ".",
                    () -> diagramService.updateRelationCardinality(item.id(), new UpdateRelationCardinalityRequest(sourceCardinality, targetCardinality)));
        }
        m = match("^(?:etiqueta|etiquetar|renombra|renombrar) (?:la )?relaci[oó]n entre (.+?) y (.+?) (?:como|a) (.+)$", text);
        if (m != null) {
            UmlRelationResponse item = findRelation(details, m.group(1), m.group(2), null);
            String label = validName(m.group(3));
            if (!supportsLabel(item.type())) throw new IllegalArgumentException("Este tipo de relación no admite etiqueta.");
            return change("UPDATE_RELATION", "Se etiquetará la relación como «" + label + "».",
                    () -> diagramService.updateRelation(item.id(), new UpdateRelationRequest(item.sourceClassId(), item.targetClassId(), item.type(),
                            label, item.sourceCardinality(), item.targetCardinality(), item.bendX(), item.bendY(), item.associationClassId(), item.alignmentPoints())));
        }
        return null;
    }

    private Plan createRelation(UUID diagramId, DiagramDetailsResponse details, String sourceName, String targetName, RelationType type,
                                String requestedSourceCardinality, String requestedTargetCardinality) {
        UmlClassResponse source = findClass(details, sourceName), target = findClass(details, targetName);
        if (!usesCardinality(type) && requestedSourceCardinality != null)
            throw new IllegalArgumentException("Este tipo de relación no admite cardinalidades.");
        String sourceCardinality = usesCardinality(type) ? requestedSourceCardinality == null ? "1..1" : cardinality(requestedSourceCardinality) : null;
        String targetCardinality = usesCardinality(type) ? requestedTargetCardinality == null ? "1..*" : cardinality(requestedTargetCardinality) : null;
        return change("CREATE_RELATION", "Se creará " + type + " entre «" + source.name() + "» y «" + target.name() + "».",
                () -> diagramService.createRelation(diagramId, new CreateRelationRequest(source.id(), target.id(), type, null, sourceCardinality, targetCardinality)));
    }

    private UmlClassResponse findClass(DiagramDetailsResponse details, String name) {
        List<UmlClassResponse> matches = details.classes().stream().filter(item -> item.name().equalsIgnoreCase(name.trim())).toList();
        if (matches.isEmpty()) throw new IllegalArgumentException("No existe la clase «" + name.trim() + "» en este diagrama.");
        if (matches.size() > 1) throw new IllegalArgumentException("Hay varias clases llamadas «" + name.trim() + "». Renómbralas en el editor antes de usar el asistente.");
        return matches.getFirst();
    }

    private UmlAttributeResponse findAttribute(UmlClassResponse owner, String name) {
        List<UmlAttributeResponse> matches = owner.attributes().stream().filter(item -> item.name().equalsIgnoreCase(name.trim())).toList();
        if (matches.size() != 1) throw new IllegalArgumentException(matches.isEmpty() ? "No existe ese atributo en «" + owner.name() + "»." : "Hay varios atributos con ese nombre.");
        return matches.getFirst();
    }

    private UmlOperationResponse findOperation(UmlClassResponse owner, String name) {
        List<UmlOperationResponse> matches = owner.operations().stream().filter(item -> item.name().equalsIgnoreCase(name.trim())).toList();
        if (matches.size() != 1) throw new IllegalArgumentException(matches.isEmpty() ? "No existe esa operación en «" + owner.name() + "»." : "Hay varias operaciones con ese nombre.");
        return matches.getFirst();
    }

    private UmlRelationResponse findRelation(DiagramDetailsResponse details, String first, String second, RelationType type) {
        UUID a = findClass(details, first).id(), b = findClass(details, second).id();
        List<UmlRelationResponse> matches = details.relations().stream().filter(item ->
                ((item.sourceClassId().equals(a) && item.targetClassId().equals(b)) ||
                 (item.sourceClassId().equals(b) && item.targetClassId().equals(a))) &&
                (type == null || item.type() == type)).toList();
        if (matches.isEmpty()) throw new IllegalArgumentException("No existe esa relación entre las clases indicadas.");
        if (matches.size() > 1) throw new IllegalArgumentException("Hay varias relaciones entre esas clases. Indica el tipo o selecciónala en el editor.");
        return matches.getFirst();
    }

    private String className(DiagramDetailsResponse details, UUID id) {
        return details.classes().stream().filter(item -> item.id().equals(id)).map(UmlClassResponse::name).findFirst().orElse("clase desconocida");
    }

    private List<UmlOperationParameterRequest> parameters(UmlOperationResponse item) {
        return item.parameters().stream().map(parameter -> new UmlOperationParameterRequest(parameter.name(), dataType(parameter.dataType()))).toList();
    }

    private Matcher match(String expression, String text) {
        Matcher matcher = Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        return matcher.matches() ? matcher : null;
    }

    private String validName(String value) {
        String name = value.trim().replaceAll("\\s+", " ");
        if (name.isBlank() || name.length() > 120) throw new IllegalArgumentException("El nombre debe tener entre 1 y 120 caracteres.");
        return name;
    }

    private double coordinate(String value) {
        double coordinate = Double.parseDouble(value);
        if (!Double.isFinite(coordinate) || coordinate > 10_000) throw new IllegalArgumentException("La posición debe estar entre 0 y 10000.");
        return coordinate;
    }

    private AttributeDataType dataType(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "STRING", "TEXTO" -> AttributeDataType.STRING;
            case "INT", "INTEGER", "ENTERO" -> AttributeDataType.INTEGER;
            case "LONG" -> AttributeDataType.LONG;
            case "DOUBLE", "DECIMAL" -> AttributeDataType.DOUBLE;
            case "BOOLEAN", "BOOL", "BOOLEANO" -> AttributeDataType.BOOLEAN;
            case "UUID" -> AttributeDataType.UUID;
            case "DATE", "LOCALDATE", "FECHA" -> AttributeDataType.LOCAL_DATE;
            case "DATETIME", "LOCALDATETIME", "FECHAHORA" -> AttributeDataType.LOCAL_DATE_TIME;
            default -> throw new IllegalArgumentException("Tipo de dato no admitido: " + value + ".");
        };
    }

    private OperationReturnType returnType(String value) {
        return value.equalsIgnoreCase("void") ? OperationReturnType.VOID : OperationReturnType.valueOf(dataType(value).name());
    }

    private RelationType relationType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "relación", "relacion", "asociación", "asociacion" -> RelationType.ASSOCIATION;
            case "agregación", "agregacion" -> RelationType.AGGREGATION;
            case "composición", "composicion" -> RelationType.COMPOSITION;
            case "herencia", "generalización", "generalizacion" -> RelationType.GENERALIZATION;
            case "realización", "realizacion" -> RelationType.REALIZATION;
            case "dependencia" -> RelationType.DEPENDENCY;
            default -> throw new IllegalArgumentException("Tipo de relación no admitido: " + value + ".");
        };
    }

    private boolean isGenericRelation(String value) { return value.equalsIgnoreCase("relación") || value.equalsIgnoreCase("relacion"); }
    private boolean usesCardinality(RelationType type) { return type == RelationType.ASSOCIATION || type == RelationType.AGGREGATION || type == RelationType.COMPOSITION; }
    private boolean supportsLabel(RelationType type) { return usesCardinality(type) || type == RelationType.DEPENDENCY; }
    private String defaultCardinality(String value) { return value == null ? "1..1" : value; }

    private String cardinality(String value) {
        if (!List.of("1..1", "0..1", "1..*", "0..*").contains(value))
            throw new IllegalArgumentException("La cardinalidad debe ser 1..1, 0..1, 1..* o 0..*.");
        return value;
    }

    private Plan info(String action, String summary) { return new Plan(action, summary, null); }
    private Plan change(String action, String summary, Runnable execute) { return new Plan(action, summary, execute); }
    public record Plan(String action, String summary, Runnable execute) {
        public boolean readOnly() { return execute == null; }
    }
}
