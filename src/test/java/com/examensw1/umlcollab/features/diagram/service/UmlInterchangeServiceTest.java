package com.examensw1.umlcollab.features.diagram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.examensw1.umlcollab.features.diagram.dto.*;
import com.examensw1.umlcollab.features.diagram.model.InterchangeFormat;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UmlInterchangeServiceTest {
    private final UmlInterchangeService service = new UmlInterchangeService();

    @Test
    void importsUmlinkXmlWithClassesAndRelations() {
        String xml = """
                <umlinkUml name="Dominio">
                  <classes><class id="user" name="Usuario" x="10" y="20" fillColor="#EAF3FF"><attribute name="email" dataType="String" visibility="PRIVATE" /></class><class id="role" name="Rol" x="200" y="20" fillColor="" /></classes>
                  <relations><relation sourceClassId="user" targetClassId="role" type="ASSOCIATION" label="tiene" sourceCardinality="1..1" targetCardinality="1..*" associationClassId="" /></relations><drawings /></umlinkUml>
                """;

        UmlInterchangeService.ImportedDiagram imported = service.parse(xml.getBytes(StandardCharsets.UTF_8), "dominio.xml");

        assertThat(imported.name()).isEqualTo("Dominio");
        assertThat(imported.classes()).hasSize(2);
        assertThat(imported.classes().getFirst().attributes()).hasSize(1);
        assertThat(imported.relations()).singleElement().extracting(UmlInterchangeService.ImportedRelation::type).isEqualTo(RelationType.ASSOCIATION);
    }

    @Test
    void importsStandardXmiSubset() {
        String xmi = """
                <xmi:XMI xmlns:xmi="http://www.omg.org/spec/XMI/20131001" xmlns:uml="http://www.eclipse.org/uml2/5.0.0/UML">
                  <uml:Model xmi:id="model" name="Ventas"><packagedElement xmi:type="uml:Class" xmi:id="invoice" name="Factura"><ownedAttribute xmi:id="number" name="numero" type="String" visibility="private" /></packagedElement><packagedElement xmi:type="uml:Class" xmi:id="line" name="Linea" /><packagedElement xmi:type="uml:Association" xmi:id="contains"><ownedEnd xmi:id="a" type="invoice" lower="1" upper="1" aggregation="none" /><ownedEnd xmi:id="b" type="line" lower="1" upper="*" aggregation="none" /></packagedElement></uml:Model>
                </xmi:XMI>
                """;

        UmlInterchangeService.ImportedDiagram imported = service.parse(xmi.getBytes(StandardCharsets.UTF_8), "ventas.xmi");

        assertThat(imported.name()).isEqualTo("Ventas");
        assertThat(imported.classes()).hasSize(2);
        assertThat(imported.relations()).singleElement().extracting(UmlInterchangeService.ImportedRelation::targetCardinality).isEqualTo("1..*");
    }

    @Test
    void rejectsExternalEntities() {
        String unsafe = "<!DOCTYPE model [<!ENTITY secret SYSTEM \"file:///secret\">]><umlinkUml name=\"&secret;\"><classes /></umlinkUml>";

        assertThatThrownBy(() -> service.parse(unsafe.getBytes(StandardCharsets.UTF_8), "unsafe.xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportsEnterpriseArchitectXmiWithVisualClassDiagram() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID diagramId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID roleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UmlClassResponse user = new UmlClassResponse(userId, diagramId, "Usuario", 120, 160, "#EAF3FF", 0L, List.of(new UmlAttributeResponse(UUID.randomUUID(), userId, "correo", "String", "PRIVATE")), List.of());
        UmlClassResponse role = new UmlClassResponse(roleId, diagramId, "Rol", 420, 160, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, userId, roleId, RelationType.ASSOCIATION, "tiene", "1..1", "1..*", null, null, null, List.of());
        DiagramDetailsResponse details = new DiagramDetailsResponse(new DiagramResponse(diagramId, projectId, "Clases", 0L, null), List.of(user, role), List.of(relation), List.of());

        String xmi = new String(service.export(details, InterchangeFormat.EA_XMI), StandardCharsets.UTF_8);
        UmlInterchangeService.ImportedDiagram imported = service.parse(xmi.getBytes(StandardCharsets.UTF_8), "clases.xmi");

        assertThat(xmi).contains("xmi:version=\"2.1\"", "<xmi:Extension extender=\"Enterprise Architect\"", "<properties name=\"Clases\" type=\"Logical\"/>", "Left=120;Top=160", "<connectors>", "multiplicity=\"1..*\"", "subject=\"EAID_");
        assertThat(imported.classes()).hasSize(2);
        assertThat(imported.relations()).singleElement().extracting(UmlInterchangeService.ImportedRelation::type).isEqualTo(RelationType.ASSOCIATION);
    }

    @Test
    void exportsEnterpriseArchitectScriptWithDiagramAndConnectors() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID diagramId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sourceId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID targetId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UmlClassResponse source = new UmlClassResponse(sourceId, diagramId, "Usuario", 120, 160, null, 0L, List.of(new UmlAttributeResponse(UUID.randomUUID(), sourceId, "correo", "String", "PRIVATE")), List.of());
        UmlClassResponse target = new UmlClassResponse(targetId, diagramId, "Rol", 420, 160, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, sourceId, targetId, RelationType.ASSOCIATION, "tiene", "1..1", "1..*", null, null, null, List.of());
        DiagramDetailsResponse details = new DiagramDetailsResponse(new DiagramResponse(diagramId, projectId, "Clases", 0L, null), List.of(source, target), List.of(relation), List.of());

        String script = new String(service.export(details, InterchangeFormat.EA_SCRIPT), StandardCharsets.UTF_8);

        assertThat(script).contains("Repository.GetTreeSelectedPackage()", "targetPackage.Elements.AddNew(name, \"Class\")", "diagram.DiagramObjects.AddNew", "source.Connectors.AddNew(name, type)", "connector.ClientEnd.Cardinality = sourceCardinality", "diagram.DiagramLinks.AddNew", "\"Usuario\"", "\"1..*\"");
    }
}
