package com.examensw1.umlcollab.features.diagram.service;

import com.examensw1.umlcollab.features.diagram.dto.*;
import com.examensw1.umlcollab.features.diagram.model.InterchangeFormat;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

@Service
@RequiredArgsConstructor
public class UmlInterchangeService {
    private static final int MAX_FILE_SIZE = 1_000_000;
    private static final int MAX_CLASSES = 200;

    public byte[] export(DiagramDetailsResponse details, InterchangeFormat format) {
        String content = switch (format) {
            case XML -> umlinkXml(details);
            case XMI -> xmi(details);
            case EA_XMI -> enterpriseArchitectXmi(details);
            case EA_SCRIPT -> enterpriseArchitectScript(details);
        };
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public ImportedDiagram parse(byte[] content, String originalFileName) {
        if (content == null || content.length == 0) throw new IllegalArgumentException("Selecciona un archivo XML o XMI con contenido.");
        if (content.length > MAX_FILE_SIZE) throw new IllegalArgumentException("El archivo supera el límite de 1 MB permitido para importar.");
        Document document = readDocument(content);
        Element root = document.getDocumentElement();
        if ("umlinkUml".equals(nameOf(root))) return parseUmlink(root);
        if ("XMI".equalsIgnoreCase(nameOf(root))) return parseXmi(root, originalFileName);
        throw new IllegalArgumentException("El archivo no corresponde a un XML UMLink ni a un XMI UML compatible.");
    }

    private Document readDocument(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        } catch (Exception ex) {
            throw new IllegalArgumentException("No pudimos leer el archivo XML/XMI. Verifica que esté bien formado.");
        }
    }

    private ImportedDiagram parseUmlink(Element root) {
        List<ImportedClass> classes = new ArrayList<>();
        for (Element item : children(findChild(root, "classes"), "class")) {
            List<ImportedAttribute> attributes = new ArrayList<>();
            List<ImportedOperation> operations = new ArrayList<>();
            for (Element attribute : children(item, "attribute")) attributes.add(new ImportedAttribute(required(attribute, "name"), value(attribute, "dataType", "String"), value(attribute, "visibility", "PRIVATE")));
            for (Element operation : children(item, "operation")) {
                List<ImportedParameter> parameters = new ArrayList<>();
                for (Element parameter : children(operation, "parameter")) parameters.add(new ImportedParameter(required(parameter, "name"), value(parameter, "dataType", "String")));
                operations.add(new ImportedOperation(required(operation, "name"), value(operation, "visibility", "PUBLIC"), value(operation, "returnType", "void"), parameters));
            }
            classes.add(new ImportedClass(required(item, "id"), required(item, "name"), number(item, "x", number(item, "positionX", classes.size() * 240d + 80)), number(item, "y", number(item, "positionY", 100)), optional(item, "fillColor"), attributes, operations));
        }
        List<ImportedRelation> relations = new ArrayList<>();
        for (Element item : children(findChild(root, "relations"), "relation")) {
            List<ImportedPoint> points = new ArrayList<>();
            for (Element point : children(item, "alignmentPoint")) points.add(new ImportedPoint(number(point, "x", 0), number(point, "y", 0)));
            relations.add(new ImportedRelation(required(item, "sourceClassId"), required(item, "targetClassId"), relationType(required(item, "type")), optional(item, "label"), optional(item, "sourceCardinality"), optional(item, "targetCardinality"), optional(item, "associationClassId"), points));
        }
        List<String> drawings = new ArrayList<>();
        for (Element item : children(findChild(root, "drawings"), "drawing")) drawings.add(required(item, "svgPath"));
        return validated(new ImportedDiagram(value(root, "name", "Diagrama importado"), classes, relations, drawings));
    }

    private ImportedDiagram parseXmi(Element root, String originalFileName) {
        Map<String, ImportedClass> classes = new LinkedHashMap<>();
        List<ImportedRelation> relations = new ArrayList<>();
        for (Element element : descendants(root)) {
            String type = typeOf(element);
            if ("uml:Class".equals(type) || "Class".equals(type)) {
                // Enterprise Architect also uses type="Class" in metadata inside
                // xmi:Extension. Only UML model elements have an XMI identifier;
                // extension metadata must not be mistaken for a class to import.
                String id = optional(element, "id");
                if (id == null) continue;
                List<ImportedAttribute> attributes = new ArrayList<>();
                List<ImportedOperation> operations = new ArrayList<>();
                for (Element child : children(element, null)) {
                    if ("ownedAttribute".equals(nameOf(child))) attributes.add(new ImportedAttribute(required(child, "name"), xmiDataType(child), xmiVisibility(child)));
                    if ("ownedOperation".equals(nameOf(child))) operations.add(xmiOperation(child));
                    if ("generalization".equals(nameOf(child)) && optional(child, "general") != null) relations.add(new ImportedRelation(id, required(child, "general"), RelationType.GENERALIZATION, null, null, null, null, List.of()));
                }
                int index = classes.size();
                classes.put(id, new ImportedClass(id, value(element, "name", "Clase"), 80 + (index % 4) * 240d, 90 + (index / 4) * 180d, null, attributes, operations));
            }
        }
        for (Element element : descendants(root)) {
            String type = typeOf(element);
            if (type.endsWith("Association")) {
                List<Element> ends = children(element, "ownedEnd");
                if (ends.size() == 2) relations.add(new ImportedRelation(required(ends.get(0), "type"), required(ends.get(1), "type"), associationType(ends), optional(element, "name"), xmiCardinality(ends.get(0)), xmiCardinality(ends.get(1)), null, List.of()));
            } else if (type.endsWith("Generalization")) {
                String specific = optional(element, "specific");
                String general = optional(element, "general");
                if (specific != null && general != null) relations.add(new ImportedRelation(specific, general, RelationType.GENERALIZATION, null, null, null, null, List.of()));
            } else if (type.endsWith("Dependency") || type.endsWith("Realization")) {
                String client = firstReference(optional(element, "client"));
                String supplier = firstReference(optional(element, "supplier"));
                if (client != null && supplier != null) relations.add(new ImportedRelation(client, supplier, type.endsWith("Realization") ? RelationType.REALIZATION : RelationType.DEPENDENCY, optional(element, "name"), null, null, null, List.of()));
            }
        }
        String fallbackName = originalFileName == null ? "Diagrama importado" : originalFileName.replaceFirst("(?i)\\.(xml|xmi)$", "");
        Element model = descendants(root).stream().filter(element -> "uml:Model".equals(typeOf(element)) || "Model".equals(typeOf(element)) || "Model".equals(nameOf(element))).findFirst().orElse(root);
        return validated(new ImportedDiagram(value(model, "name", fallbackName), new ArrayList<>(classes.values()), relations, List.of()));
    }

    private ImportedOperation xmiOperation(Element operation) {
        List<ImportedParameter> parameters = new ArrayList<>();
        String returnType = "void";
        for (Element parameter : children(operation, "ownedParameter")) {
            if ("return".equals(value(parameter, "direction", "in"))) returnType = xmiDataType(parameter);
            else parameters.add(new ImportedParameter(required(parameter, "name"), xmiDataType(parameter)));
        }
        return new ImportedOperation(required(operation, "name"), xmiVisibility(operation), returnType, parameters);
    }

    private ImportedDiagram validated(ImportedDiagram diagram) {
        if (diagram.classes().isEmpty()) throw new IllegalArgumentException("El archivo no contiene clases UML compatibles.");
        if (diagram.classes().size() > MAX_CLASSES) throw new IllegalArgumentException("El archivo contiene más de 200 clases; reduce el modelo antes de importarlo.");
        Set<String> keys = new HashSet<>();
        for (ImportedClass item : diagram.classes()) {
            if (!keys.add(item.key())) throw new IllegalArgumentException("El archivo contiene identificadores de clase duplicados.");
            validateText(item.name(), "El nombre de una clase", 120);
            if (item.attributes().size() > 100 || item.operations().size() > 100) throw new IllegalArgumentException("Cada clase admite hasta 100 atributos y 100 operaciones al importar.");
            item.attributes().forEach(attribute -> validateText(attribute.name(), "El nombre de un atributo", 120));
            item.operations().forEach(operation -> { validateText(operation.name(), "El nombre de una operación", 120); if (operation.parameters().size() > 10) throw new IllegalArgumentException("Cada operación admite hasta 10 parámetros."); });
        }
        for (ImportedRelation relation : diagram.relations()) {
            if (!keys.contains(relation.sourceKey()) || !keys.contains(relation.targetKey())) throw new IllegalArgumentException("Una relación referencia una clase inexistente en el archivo.");
        }
        return diagram;
    }

    private String umlinkXml(DiagramDetailsResponse details) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<umlinkUml version=\"1.0\" name=\"").append(escape(details.diagram().name())).append("\">\n  <classes>\n");
        for (UmlClassResponse item : details.classes()) {
            xml.append("    <class id=\"").append(item.id()).append("\" name=\"").append(escape(item.name())).append("\" x=\"").append(item.positionX()).append("\" y=\"").append(item.positionY()).append("\" fillColor=\"").append(escape(item.fillColor())).append("\">\n");
            for (UmlAttributeResponse attribute : item.attributes()) xml.append("      <attribute name=\"").append(escape(attribute.name())).append("\" dataType=\"").append(escape(attribute.dataType())).append("\" visibility=\"").append(escape(attribute.visibility())).append("\" />\n");
            for (UmlOperationResponse operation : item.operations()) { xml.append("      <operation name=\"").append(escape(operation.name())).append("\" visibility=\"").append(escape(operation.visibility())).append("\" returnType=\"").append(escape(operation.returnType())).append("\">\n"); for (UmlOperationParameterResponse parameter : operation.parameters()) xml.append("        <parameter name=\"").append(escape(parameter.name())).append("\" dataType=\"").append(escape(parameter.dataType())).append("\" />\n"); xml.append("      </operation>\n"); }
            xml.append("    </class>\n");
        }
        xml.append("  </classes>\n  <relations>\n");
        for (UmlRelationResponse item : details.relations()) { xml.append("    <relation sourceClassId=\"").append(item.sourceClassId()).append("\" targetClassId=\"").append(item.targetClassId()).append("\" type=\"").append(item.type()).append("\" label=\"").append(escape(item.label())).append("\" sourceCardinality=\"").append(escape(item.sourceCardinality())).append("\" targetCardinality=\"").append(escape(item.targetCardinality())).append("\" associationClassId=\"").append(item.associationClassId() == null ? "" : item.associationClassId()).append("\">\n"); for (RelationAlignmentPoint point : item.alignmentPoints()) xml.append("      <alignmentPoint x=\"").append(point.x()).append("\" y=\"").append(point.y()).append("\" />\n"); xml.append("    </relation>\n"); }
        xml.append("  </relations>\n  <drawings>\n");
        for (DiagramDrawingResponse drawing : details.drawings()) xml.append("    <drawing svgPath=\"").append(escape(drawing.svgPath())).append("\" />\n");
        return xml.append("  </drawings>\n</umlinkUml>\n").toString();
    }

    private String xmi(DiagramDetailsResponse details) {
        String modelId = "model_" + details.diagram().id();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xmi:XMI xmlns:xmi=\"http://www.omg.org/spec/XMI/20131001\" xmlns:uml=\"http://www.eclipse.org/uml2/5.0.0/UML\">\n  <uml:Model xmi:id=\"").append(modelId).append("\" name=\"").append(escape(details.diagram().name())).append("\">\n");
        for (UmlClassResponse item : details.classes()) { xml.append("    <packagedElement xmi:type=\"uml:Class\" xmi:id=\"").append(xmiId(item.id())).append("\" name=\"").append(escape(item.name())).append("\">\n"); for (UmlAttributeResponse attribute : item.attributes()) xml.append("      <ownedAttribute xmi:id=\"").append(xmiId(attribute.id())).append("\" name=\"").append(escape(attribute.name())).append("\" type=\"").append(escape(attribute.dataType())).append("\" visibility=\"").append(visibility(attribute.visibility())).append("\" />\n"); for (UmlOperationResponse operation : item.operations()) { xml.append("      <ownedOperation xmi:id=\"").append(xmiId(operation.id())).append("\" name=\"").append(escape(operation.name())).append("\" visibility=\"").append(visibility(operation.visibility())).append("\">\n"); for (UmlOperationParameterResponse parameter : operation.parameters()) xml.append("        <ownedParameter xmi:id=\"").append(xmiId(parameter.id())).append("\" name=\"").append(escape(parameter.name())).append("\" type=\"").append(escape(parameter.dataType())).append("\" direction=\"in\" />\n"); xml.append("        <ownedParameter xmi:id=\"return_").append(xmiId(operation.id())).append("\" type=\"").append(escape(operation.returnType())).append("\" direction=\"return\" />\n      </ownedOperation>\n"); } xml.append("    </packagedElement>\n"); }
        for (UmlRelationResponse item : details.relations()) {
            if (item.type() == RelationType.GENERALIZATION) continue;
            String type = switch (item.type()) { case REALIZATION -> "uml:Realization"; case DEPENDENCY -> "uml:Dependency"; default -> "uml:Association"; };
            xml.append("    <packagedElement xmi:type=\"").append(type).append("\" xmi:id=\"").append(xmiId(item.id())).append("\" name=\"").append(escape(item.label())).append("\"");
            if (item.type() == RelationType.REALIZATION || item.type() == RelationType.DEPENDENCY) xml.append(" client=\"").append(xmiId(item.sourceClassId())).append("\" supplier=\"").append(xmiId(item.targetClassId())).append("\" />\n");
            else { xml.append(">\n      <ownedEnd xmi:id=\"end_source_").append(xmiId(item.id())).append("\" type=\"").append(xmiId(item.sourceClassId())).append("\" lower=\"").append(lower(item.sourceCardinality())).append("\" upper=\"").append(upper(item.sourceCardinality())).append("\" aggregation=\"").append(aggregation(item.type())).append("\" />\n      <ownedEnd xmi:id=\"end_target_").append(xmiId(item.id())).append("\" type=\"").append(xmiId(item.targetClassId())).append("\" lower=\"").append(lower(item.targetCardinality())).append("\" upper=\"").append(upper(item.targetCardinality())).append("\" aggregation=\"none\" />\n    </packagedElement>\n"); }
        }
        for (UmlRelationResponse item : details.relations()) if (item.type() == RelationType.GENERALIZATION) xml.append("    <packagedElement xmi:type=\"uml:Generalization\" xmi:id=\"").append(xmiId(item.id())).append("\" specific=\"").append(xmiId(item.sourceClassId())).append("\" general=\"").append(xmiId(item.targetClassId())).append("\" />\n");
        return xml.append("  </uml:Model>\n</xmi:XMI>\n").toString();
    }

    private String enterpriseArchitectXmi(DiagramDetailsResponse details) {
        String packageId = eaPackageId(details.diagram().id());
        String diagramId = eaDiagramId(details.diagram().id());
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xmi:XMI xmi:version=\"2.1\" xmlns:uml=\"http://schema.omg.org/spec/UML/2.1\" xmlns:xmi=\"http://schema.omg.org/spec/XMI/2.1\">\n  <xmi:Documentation exporter=\"Enterprise Architect\" exporterVersion=\"15.0\"/>\n  <uml:Model xmi:type=\"uml:Model\" name=\"").append(escape(details.diagram().name())).append("\" visibility=\"public\">\n    <packagedElement xmi:type=\"uml:Package\" xmi:id=\"").append(packageId).append("\" name=\"").append(escape(details.diagram().name())).append("\" visibility=\"public\">\n");
        appendEaClasses(xml, details.classes());
        appendEaRelations(xml, details.relations());
        xml.append("    </packagedElement>\n  </uml:Model>\n  <xmi:Extension extender=\"Enterprise Architect\" extenderID=\"6.5\">\n    <elements>\n      <element xmi:idref=\"").append(packageId).append("\" xmi:type=\"uml:Package\" name=\"").append(escape(details.diagram().name())).append("\" scope=\"public\">\n        <model package2=\"").append(packageId).append("\" package=\"").append(packageId).append("\" tpos=\"0\" ea_localid=\"1\" ea_eleType=\"package\"/>\n        <properties isSpecification=\"false\" sType=\"Package\" nType=\"0\" scope=\"public\"/>\n      </element>\n");
        appendEaClassElements(xml, details.classes(), packageId, details.diagram().name());
        xml.append("    </elements>\n    <connectors>\n");
        appendEaConnectors(xml, details.relations(), details.classes(), diagramId);
        xml.append("    </connectors>\n    <diagrams>\n      <diagram xmi:id=\"").append(diagramId).append("\">\n        <model package=\"").append(packageId).append("\" localID=\"1\" owner=\"").append(packageId).append("\"/>\n        <properties name=\"").append(escape(details.diagram().name())).append("\" type=\"Logical\"/>\n        <elements>\n");
        appendEaDiagramElements(xml, details.classes(), details.relations());
        return xml.append("        </elements>\n      </diagram>\n    </diagrams>\n  </xmi:Extension>\n</xmi:XMI>\n").toString();
    }

    private String enterpriseArchitectScript(DiagramDetailsResponse details) {
        StringBuilder script = new StringBuilder("""
                /*
                 * UMLink - Enterprise Architect 15 importer
                 * Select a destination package in EA, then run this script from
                 * Specialize > Scripting. It creates a child package and a Logical diagram.
                 */
                function save(item, context) {
                    if (!item.Update()) {
                        throw new Error(context + ": " + item.GetLastError());
                    }
                }

                function main() {
                    var parentPackage = Repository.GetTreeSelectedPackage();
                    if (parentPackage == null) {
                        Session.Prompt("Selecciona un paquete destino en el navegador de Enterprise Architect y vuelve a ejecutar el script.", promptOK);
                        return;
                    }

                    var targetPackage = parentPackage.Packages.AddNew(
                """);
        script.append(js(details.diagram().name())).append("""
                , "");
                    save(targetPackage, "No se pudo crear el paquete UMLink");
                    parentPackage.Packages.Refresh();

                    var diagram = targetPackage.Diagrams.AddNew(
                """);
        script.append(js(details.diagram().name())).append("""
                , "Logical");
                    save(diagram, "No se pudo crear el diagrama");
                    var elements = {};

                    function addClass(key, name, left, top) {
                        var element = targetPackage.Elements.AddNew(name, "Class");
                        save(element, "No se pudo crear la clase " + name);
                        var view = diagram.DiagramObjects.AddNew("l=" + left + ";r=" + (left + 220) + ";t=" + top + ";b=" + (top + 120) + ";", "");
                        view.ElementID = element.ElementID;
                        save(view, "No se pudo posicionar la clase " + name);
                        elements[key] = element;
                        return element;
                    }

                    function addAttribute(element, name, dataType, visibility) {
                        var attribute = element.Attributes.AddNew(name, dataType);
                        attribute.Visibility = visibility;
                        save(attribute, "No se pudo crear el atributo " + name);
                        element.Attributes.Refresh();
                    }

                    function addOperation(element, name, returnType, visibility) {
                        var operation = element.Methods.AddNew(name, returnType);
                        operation.ReturnType = returnType;
                        operation.Visibility = visibility;
                        save(operation, "No se pudo crear la operación " + name);
                        element.Methods.Refresh();
                        return operation;
                    }

                    function addParameter(operation, name, dataType) {
                        var parameter = operation.Parameters.AddNew(name, dataType);
                        parameter.Kind = "in";
                        save(parameter, "No se pudo crear el parámetro " + name);
                        operation.Parameters.Refresh();
                    }

                    function addConnector(sourceKey, targetKey, name, type, sourceCardinality, targetCardinality, sourceAggregation, associationClassKey) {
                        var source = elements[sourceKey];
                        var target = elements[targetKey];
                        if (source == null || target == null) throw new Error("La relación referencia una clase inexistente.");
                        var connector = source.Connectors.AddNew(name, type);
                        connector.SupplierID = target.ElementID;
                        save(connector, "No se pudo crear la relación " + type);
                        connector.ClientEnd.Cardinality = sourceCardinality;
                        connector.SupplierEnd.Cardinality = targetCardinality;
                        connector.ClientEnd.Aggregation = sourceAggregation;
                        save(connector.ClientEnd, "No se pudo configurar el extremo de origen");
                        save(connector.SupplierEnd, "No se pudo configurar el extremo de destino");
                        connector.DiagramID = diagram.DiagramID;
                        save(connector, "No se pudo asociar la relación al diagrama");
                        var link = diagram.DiagramLinks.AddNew("", "");
                        link.ConnectorID = connector.ConnectorID;
                        link.DiagramID = diagram.DiagramID;
                        link.LineStyle = 1;
                        link.IsHidden = false;
                        save(link, "No se pudo dibujar la relación");
                        if (associationClassKey != "") {
                            var associationClass = elements[associationClassKey];
                            if (associationClass == null || !associationClass.CreateAssociationClass(connector.ConnectorID)) {
                                throw new Error("No se pudo vincular la clase de asociación.");
                            }
                        }
                        source.Connectors.Refresh();
                        diagram.DiagramLinks.Refresh();
                    }
                """);

        for (UmlClassResponse item : details.classes()) {
            script.append("    var class_").append(item.id().toString().replace('-', '_')).append(" = addClass(").append(js(item.id().toString())).append(", ").append(js(item.name())).append(", ").append(coordinate(item.positionX())).append(", ").append(coordinate(item.positionY())).append(");\n");
            for (UmlAttributeResponse attribute : item.attributes()) script.append("    addAttribute(class_").append(item.id().toString().replace('-', '_')).append(", ").append(js(attribute.name())).append(", ").append(js(attribute.dataType())).append(", ").append(js(scriptVisibility(attribute.visibility()))).append(");\n");
            for (UmlOperationResponse operation : item.operations()) {
                String operationVariable = "operation_" + operation.id().toString().replace('-', '_');
                script.append("    var ").append(operationVariable).append(" = addOperation(class_").append(item.id().toString().replace('-', '_')).append(", ").append(js(operation.name())).append(", ").append(js(operation.returnType())).append(", ").append(js(scriptVisibility(operation.visibility()))).append(");\n");
                for (UmlOperationParameterResponse parameter : operation.parameters()) script.append("    addParameter(").append(operationVariable).append(", ").append(js(parameter.name())).append(", ").append(js(parameter.dataType())).append(");\n");
            }
        }
        for (UmlRelationResponse relation : details.relations()) {
            script.append("    addConnector(").append(js(relation.sourceClassId().toString())).append(", ").append(js(relation.targetClassId().toString())).append(", ").append(js(relation.label())).append(", ").append(js(eaConnectorType(relation.type()))).append(", ").append(js(relation.sourceCardinality())).append(", ").append(js(relation.targetCardinality())).append(", ").append(eaAggregationValue(relation.type())).append(", ").append(js(relation.associationClassId() == null ? "" : relation.associationClassId().toString())).append(");\n");
        }
        return script.append("""
                    diagram.Update();
                    Repository.ReloadDiagram(diagram.DiagramID);
                    Session.Prompt("Diagrama importado correctamente: " + diagram.Name, promptOK);
                }

                main();
                """).toString();
    }

    private String js(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    private int coordinate(double value) { return Math.max(0, (int) Math.round(value)); }

    private String scriptVisibility(String visibility) { return switch (visibility == null ? "" : visibility.toUpperCase(Locale.ROOT)) { case "PRIVATE" -> "Private"; case "PROTECTED" -> "Protected"; default -> "Public"; }; }

    private int eaAggregationValue(RelationType type) { return switch (type) { case AGGREGATION -> 1; case COMPOSITION -> 2; default -> 0; }; }

    private void appendEaClasses(StringBuilder xml, List<UmlClassResponse> classes) {
        for (UmlClassResponse item : classes) {
            xml.append("      <packagedElement xmi:type=\"uml:Class\" xmi:id=\"").append(eaId(item.id())).append("\" name=\"").append(escape(item.name())).append("\" visibility=\"public\">\n");
            for (UmlAttributeResponse attribute : item.attributes()) xml.append("        <ownedAttribute xmi:type=\"uml:Property\" xmi:id=\"").append(eaId(attribute.id())).append("\" name=\"").append(escape(attribute.name())).append("\" type=\"").append(escape(attribute.dataType())).append("\" visibility=\"").append(visibility(attribute.visibility())).append("\"/>\n");
            for (UmlOperationResponse operation : item.operations()) {
                xml.append("        <ownedOperation xmi:type=\"uml:Operation\" xmi:id=\"").append(eaId(operation.id())).append("\" name=\"").append(escape(operation.name())).append("\" visibility=\"").append(visibility(operation.visibility())).append("\">\n");
                for (UmlOperationParameterResponse parameter : operation.parameters()) xml.append("          <ownedParameter xmi:type=\"uml:Parameter\" xmi:id=\"").append(eaId(parameter.id())).append("\" name=\"").append(escape(parameter.name())).append("\" type=\"").append(escape(parameter.dataType())).append("\" direction=\"in\"/>\n");
                xml.append("          <ownedParameter xmi:type=\"uml:Parameter\" xmi:id=\"return_").append(eaId(operation.id())).append("\" type=\"").append(escape(operation.returnType())).append("\" direction=\"return\"/>\n        </ownedOperation>\n");
            }
            xml.append("      </packagedElement>\n");
        }
    }

    private void appendEaClassElements(StringBuilder xml, List<UmlClassResponse> classes, String packageId, String packageName) {
        for (int index = 0; index < classes.size(); index++) {
            UmlClassResponse item = classes.get(index);
            xml.append("      <element xmi:idref=\"").append(eaId(item.id())).append("\" xmi:type=\"uml:Class\" name=\"").append(escape(item.name())).append("\" scope=\"public\">\n")
                    .append("        <model package=\"").append(packageId).append("\" tpos=\"0\" ea_localid=\"").append(index + 2).append("\" ea_eleType=\"element\"/>\n")
                    .append("        <properties isSpecification=\"false\" sType=\"Class\" nType=\"0\" scope=\"public\"/>\n")
                    .append("        <code gentype=\"&lt;none&gt;\"/>\n")
                    .append("        <style appearance=\"BackColor=-1;BorderColor=-1;BorderWidth=-1;FontColor=-1;BorderStyle=0;\"/>\n")
                    .append("        <tags/><xrefs/><extendedProperties tagged=\"0\" package_name=\"").append(escape(packageName)).append("\"/>\n")
                    .append("      </element>\n");
        }
    }

    private void appendEaRelations(StringBuilder xml, List<UmlRelationResponse> relations) {
        for (UmlRelationResponse item : relations) {
            if (item.type() == RelationType.GENERALIZATION) {
                xml.append("      <packagedElement xmi:type=\"uml:Generalization\" xmi:id=\"").append(eaId(item.id())).append("\" specific=\"").append(eaId(item.sourceClassId())).append("\" general=\"").append(eaId(item.targetClassId())).append("\"/>\n");
                continue;
            }
            String type = switch (item.type()) { case REALIZATION -> "uml:Realization"; case DEPENDENCY -> "uml:Dependency"; default -> "uml:Association"; };
            xml.append("      <packagedElement xmi:type=\"").append(type).append("\" xmi:id=\"").append(eaId(item.id())).append("\" name=\"").append(escape(item.label())).append("\"");
            if (item.type() == RelationType.REALIZATION || item.type() == RelationType.DEPENDENCY) {
                xml.append(" client=\"").append(eaId(item.sourceClassId())).append("\" supplier=\"").append(eaId(item.targetClassId())).append("\"/>\n");
            } else {
                xml.append(">\n        <ownedEnd xmi:type=\"uml:Property\" xmi:id=\"end_source_").append(eaId(item.id())).append("\" type=\"").append(eaId(item.sourceClassId())).append("\" lower=\"").append(lower(item.sourceCardinality())).append("\" upper=\"").append(upper(item.sourceCardinality())).append("\" aggregation=\"").append(aggregation(item.type())).append("\"/>\n        <ownedEnd xmi:type=\"uml:Property\" xmi:id=\"end_target_").append(eaId(item.id())).append("\" type=\"").append(eaId(item.targetClassId())).append("\" lower=\"").append(lower(item.targetCardinality())).append("\" upper=\"").append(upper(item.targetCardinality())).append("\" aggregation=\"none\"/>\n      </packagedElement>\n");
            }
        }
    }

    private void appendEaDiagramElements(StringBuilder xml, List<UmlClassResponse> classes, List<UmlRelationResponse> relations) {
        int sequence = 1;
        for (UmlClassResponse item : classes) {
            int left = (int) Math.round(item.positionX());
            int top = (int) Math.round(item.positionY());
            int height = 95 + item.attributes().size() * 20 + item.operations().size() * 20;
            xml.append("          <element geometry=\"Left=").append(left).append(";Top=").append(top).append(";Right=").append(left + 220).append(";Bottom=").append(top + height).append(";\" subject=\"").append(eaId(item.id())).append("\" seqno=\"").append(sequence++).append("\" style=\";\"/>\n");
        }
        for (UmlRelationResponse item : relations) xml.append("          <element geometry=\"SX=0;SY=0;EX=0;EY=0;Path=;\" subject=\"").append(eaId(item.id())).append("\" style=\";Hidden=0;\"/>\n");
    }

    private void appendEaConnectors(StringBuilder xml, List<UmlRelationResponse> relations, List<UmlClassResponse> classes, String diagramId) {
        Map<UUID, UmlClassResponse> classesById = new HashMap<>();
        Map<UUID, Integer> localIds = new HashMap<>();
        for (int index = 0; index < classes.size(); index++) {
            classesById.put(classes.get(index).id(), classes.get(index));
            localIds.put(classes.get(index).id(), index + 2);
        }
        for (UmlRelationResponse relation : relations) {
            UmlClassResponse source = classesById.get(relation.sourceClassId());
            UmlClassResponse target = classesById.get(relation.targetClassId());
            if (source == null || target == null) continue;
            xml.append("      <connector xmi:idref=\"").append(eaId(relation.id())).append("\">\n        <source xmi:idref=\"").append(eaId(source.id())).append("\">\n          <model ea_localid=\"").append(localIds.get(source.id())).append("\" type=\"Class\" name=\"").append(escape(source.name())).append("\"/>\n          <role visibility=\"Public\" targetScope=\"instance\" multiplicity=\"").append(escape(relation.sourceCardinality())).append("\"/>\n          <type aggregation=\"").append(aggregation(relation.type())).append("\" containment=\"Unspecified\"/>\n          <modifiers isOrdered=\"false\" changeable=\"none\" isNavigable=\"false\"/>\n        </source>\n        <target xmi:idref=\"").append(eaId(target.id())).append("\">\n          <model ea_localid=\"").append(localIds.get(target.id())).append("\" type=\"Class\" name=\"").append(escape(target.name())).append("\"/>\n          <role visibility=\"Public\" targetScope=\"instance\" multiplicity=\"").append(escape(relation.targetCardinality())).append("\"/>\n          <type aggregation=\"none\" containment=\"Unspecified\"/>\n          <modifiers isOrdered=\"false\" changeable=\"none\" isNavigable=\"true\"/>\n        </target>\n        <model ea_localid=\"1\"/>\n        <properties name=\"").append(escape(relation.label())).append("\" ea_type=\"").append(eaConnectorType(relation.type())).append("\" direction=\"Source -&gt; Destination\"/>\n        <appearance linemode=\"3\" linecolor=\"-1\" linewidth=\"0\" headStyle=\"0\" lineStyle=\"0\"/>\n        <labels mt=\"").append(escape(relation.label())).append("\"/>\n        <extendedProperties diagram=\"").append(diagramId).append("\"/>\n      </connector>\n");
        }
    }

    private String xmiDataType(Element item) { return normalizeType(value(item, "type", "String")); }
    private String xmiVisibility(Element item) { String value = value(item, "visibility", "public"); return switch (value.toLowerCase(Locale.ROOT)) { case "private" -> "PRIVATE"; case "protected" -> "PROTECTED"; default -> "PUBLIC"; }; }
    private String xmiCardinality(Element item) { String lower = value(item, "lower", "1"); String upper = value(item, "upper", "1"); if ("1".equals(lower) && "1".equals(upper)) return "1..1"; if ("0".equals(lower) && "1".equals(upper)) return "0..1"; if ("1".equals(lower) && "*".equals(upper)) return "1..*"; throw new IllegalArgumentException("La cardinalidad XMI " + lower + ".." + upper + " no está soportada. Usa 1..1, 0..1 o 1..*."); }
    private RelationType associationType(List<Element> ends) { String aggregation = value(ends.get(0), "aggregation", "none"); return "composite".equals(aggregation) ? RelationType.COMPOSITION : "shared".equals(aggregation) ? RelationType.AGGREGATION : RelationType.ASSOCIATION; }
    private RelationType relationType(String value) { try { return RelationType.valueOf(value.toUpperCase(Locale.ROOT)); } catch (RuntimeException ex) { throw new IllegalArgumentException("El archivo contiene un tipo de relación UML no compatible: " + value); } }
    private String normalizeType(String value) { String normalized = value == null ? "" : value.substring(value.lastIndexOf(':') + 1).toLowerCase(Locale.ROOT); return switch (normalized) { case "string" -> "String"; case "integer", "int" -> "Integer"; case "long" -> "Long"; case "double", "float", "real" -> "Double"; case "boolean", "bool" -> "Boolean"; case "uuid" -> "UUID"; case "localdate", "date" -> "LocalDate"; case "localdatetime", "datetime" -> "LocalDateTime"; case "void", "" -> "void"; default -> "String"; }; }
    private String identifier(Element item) { String id = optional(item, "id"); if (id == null) throw new IllegalArgumentException("Una clase XMI no contiene xmi:id."); return id; }
    private String typeOf(Element item) { String type = optional(item, "type"); return type == null ? "" : type; }
    private String firstReference(String value) { return value == null || value.isBlank() ? null : value.trim().split("\\s+")[0]; }
    private List<Element> descendants(Element root) { List<Element> output = new ArrayList<>(); NodeList nodes = root.getElementsByTagName("*"); for (int index = 0; index < nodes.getLength(); index++) output.add((Element) nodes.item(index)); return output; }
    private List<Element> children(Element parent, String expectedName) { if (parent == null) return List.of(); List<Element> output = new ArrayList<>(); NodeList nodes = parent.getChildNodes(); for (int index = 0; index < nodes.getLength(); index++) if (nodes.item(index) instanceof Element item && (expectedName == null || expectedName.equals(nameOf(item)))) output.add(item); return output; }
    private Element findChild(Element parent, String expectedName) { return children(parent, expectedName).stream().findFirst().orElse(null); }
    private String nameOf(Element item) { return item.getLocalName() == null ? item.getNodeName().replaceFirst("^.*:", "") : item.getLocalName(); }
    private String optional(Element item, String attribute) { if (item == null) return null; String value = item.getAttribute(attribute); if (value == null || value.isBlank()) value = item.getAttributeNS("http://www.omg.org/spec/XMI/20131001", attribute); if (value == null || value.isBlank()) value = item.getAttributeNS("http://schema.omg.org/spec/XMI/2.1", attribute); return value == null || value.isBlank() ? null : value.trim(); }
    private String required(Element item, String attribute) { String value = optional(item, attribute); if (value == null) throw new IllegalArgumentException("Falta el atributo obligatorio '" + attribute + "' en el archivo importado."); return value; }
    private String value(Element item, String attribute, String fallback) { String value = optional(item, attribute); return value == null ? fallback : value; }
    private double number(Element item, String attribute, double fallback) { try { return Double.parseDouble(value(item, attribute, Double.toString(fallback))); } catch (NumberFormatException ex) { throw new IllegalArgumentException("La posición '" + attribute + "' no es válida."); } }
    private void validateText(String value, String label, int max) { if (value == null || value.isBlank() || value.length() > max) throw new IllegalArgumentException(label + " debe tener entre 1 y " + max + " caracteres."); }
    private String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
    private String xmiId(Object id) { return "id_" + id.toString().replaceAll("[^A-Za-z0-9_.-]", "_"); }
    private String eaId(Object id) { return "EAID_" + id.toString().replace('-', '_').toUpperCase(Locale.ROOT); }
    private String eaPackageId(Object id) { return "EAPK_" + id.toString().replace('-', '_').toUpperCase(Locale.ROOT); }
    private String eaDiagramId(Object id) { return "EAID_DIAGRAM_" + id.toString().replace('-', '_').toUpperCase(Locale.ROOT); }
    private String visibility(String value) { return value == null ? "public" : value.toLowerCase(Locale.ROOT); }
    private String lower(String value) { return "0..1".equals(value) ? "0" : "1"; }
    private String upper(String value) { return "1..*".equals(value) ? "*" : "1"; }
    private String aggregation(RelationType type) { return type == RelationType.COMPOSITION ? "composite" : type == RelationType.AGGREGATION ? "shared" : "none"; }
    private String eaConnectorType(RelationType type) { return switch (type) { case GENERALIZATION -> "Generalization"; case REALIZATION -> "Realisation"; case DEPENDENCY -> "Dependency"; case AGGREGATION, COMPOSITION, ASSOCIATION -> "Association"; }; }

    public record ImportedDiagram(String name, List<ImportedClass> classes, List<ImportedRelation> relations, List<String> drawings) {}
    public record ImportedClass(String key, String name, double x, double y, String fillColor, List<ImportedAttribute> attributes, List<ImportedOperation> operations) {}
    public record ImportedAttribute(String name, String dataType, String visibility) {}
    public record ImportedOperation(String name, String visibility, String returnType, List<ImportedParameter> parameters) {}
    public record ImportedParameter(String name, String dataType) {}
    public record ImportedRelation(String sourceKey, String targetKey, RelationType type, String label, String sourceCardinality, String targetCardinality, String associationClassKey, List<ImportedPoint> alignmentPoints) {}
    public record ImportedPoint(double x, double y) {}
}
