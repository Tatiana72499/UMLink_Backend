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
    void importsClassicEnterpriseArchitectXmi() {
        String xmi = """
                <XMI xmlns:xmi="http://www.omg.org/XMI" xmlns:UML="org.omg.xmi.namespace.UML">
                  <XMI.content><UML:Model xmi.id="model" name="Biblioteca"><UML:Namespace.ownedElement>
                    <UML:Class xmi.id="book" name="Libro"><UML:Classifier.feature><UML:Attribute xmi.id="title" name="titulo" type="String" visibility="private" /></UML:Classifier.feature></UML:Class>
                    <UML:Class xmi.id="author" name="Autor" />
                    <UML:Association xmi.id="writes" name="escribe"><UML:Association.connection>
                      <UML:AssociationEnd xmi.id="end1" type="author" multiplicity="1" />
                      <UML:AssociationEnd xmi.id="end2" type="book" multiplicity="1..*" />
                    </UML:Association.connection></UML:Association>
                    <UML:Association xmi.id="metadata"><UML:Association.connection>
                      <UML:AssociationEnd xmi.id="end3" type="author" multiplicity="1" />
                      <UML:AssociationEnd xmi.id="end4" type="ea_internal_metadata" multiplicity="1" />
                    </UML:Association.connection></UML:Association>
                  </UML:Namespace.ownedElement></UML:Model></XMI.content>
                </XMI>
                """;

        UmlInterchangeService.ImportedDiagram imported = service.parse(xmi.getBytes(StandardCharsets.UTF_8), "biblioteca.xmi");

        assertThat(imported.name()).isEqualTo("Biblioteca");
        assertThat(imported.classes()).extracting(UmlInterchangeService.ImportedClass::name).containsExactly("Libro", "Autor");
        assertThat(imported.classes().getFirst().attributes()).singleElement().extracting(UmlInterchangeService.ImportedAttribute::name).isEqualTo("titulo");
        assertThat(imported.relations()).singleElement().satisfies(relation -> {
            assertThat(relation.sourceKey()).isEqualTo("author");
            assertThat(relation.targetKey()).isEqualTo("book");
            assertThat(relation.targetCardinality()).isEqualTo("1..*");
        });
    }

    @Test
    void importsEnterpriseArchitectConnectorMetadataAsRelation() {
        String xmi = """
                <xmi:XMI xmlns:xmi="http://www.omg.org/spec/XMI/20131001" xmlns:uml="http://www.eclipse.org/uml2/5.0.0/UML">
                  <uml:Model xmi:id="model" name="Dominio">
                    <packagedElement xmi:type="uml:Class" xmi:id="student" name="Estudiante" />
                    <packagedElement xmi:type="uml:Class" xmi:id="course" name="Curso" />
                  </uml:Model>
                  <xmi:Extension extender="Enterprise Architect"><connectors><connector>
                    <source xmi:idref="student"><role multiplicity="1..*" /><type aggregation="none" /></source>
                    <target xmi:idref="course"><role multiplicity="1..*" /><type aggregation="none" /></target>
                    <properties name="cursa" ea_type="Association" />
                  </connector></connectors></xmi:Extension>
                </xmi:XMI>
                """;

        UmlInterchangeService.ImportedDiagram imported = service.parse(xmi.getBytes(StandardCharsets.UTF_8), "dominio-ea.xmi");

        assertThat(imported.relations()).singleElement().satisfies(relation -> {
            assertThat(relation.sourceKey()).isEqualTo("student");
            assertThat(relation.targetKey()).isEqualTo("course");
            assertThat(relation.sourceCardinality()).isEqualTo("1..*");
            assertThat(relation.targetCardinality()).isEqualTo("1..*");
            assertThat(relation.label()).isEqualTo("cursa");
        });
    }

    @Test
    void rejectsExternalEntities() {
        String unsafe = "<!DOCTYPE model [<!ENTITY secret SYSTEM \"file:///secret\">]><umlinkUml name=\"&secret;\"><classes /></umlinkUml>";

        assertThatThrownBy(() -> service.parse(unsafe.getBytes(StandardCharsets.UTF_8), "unsafe.xml"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void importsPlantUmlClassesAttributesAndRelations() {
        String plantUml = """
                @startuml
                class "Usuario" as user #EAF3FF {
                  - id : UUID <<PK>>
                  + registrar(nombre: String) : void
                }
                class Rol
                user "1..1" -- "1..*" Rol : tiene
                @enduml
                """;

        UmlInterchangeService.ImportedDiagram imported = service.parse(plantUml.getBytes(StandardCharsets.UTF_8), "usuarios.puml");

        assertThat(imported.classes()).hasSize(2);
        assertThat(imported.classes().getFirst().attributes()).singleElement().extracting(UmlInterchangeService.ImportedAttribute::primaryKey).isEqualTo(true);
        assertThat(imported.classes().getFirst().operations()).singleElement().extracting(UmlInterchangeService.ImportedOperation::name).isEqualTo("registrar");
        assertThat(imported.relations()).singleElement().extracting(UmlInterchangeService.ImportedRelation::type).isEqualTo(RelationType.ASSOCIATION);
    }

    @Test
    void importsCommonAiAttributeVariants() {
        String plantUml = """
                @startuml
                class Usuario {
                  private id: int <<PK>>
                  + nombre : String = \"sin nombre\"
                  # creadoEn: java.time.LocalDateTime {readOnly}
                }
                @enduml
                """;

        UmlInterchangeService.ImportedClass imported = service.parse(plantUml.getBytes(StandardCharsets.UTF_8), "ia.puml").classes().getFirst();

        assertThat(imported.attributes()).extracting(UmlInterchangeService.ImportedAttribute::name).containsExactly("id", "nombre", "creadoEn");
        assertThat(imported.attributes().getFirst().primaryKey()).isTrue();
        assertThat(imported.attributes()).extracting(UmlInterchangeService.ImportedAttribute::visibility).containsExactly("PRIVATE", "PUBLIC", "PROTECTED");
        assertThat(imported.attributes().get(2).dataType()).isEqualTo("LocalDateTime");
    }

    @Test
    void importsUntypedAttributesGeneratedFromAnImageAsStrings() {
        String plantUml = """
                @startuml
                class Libro {
                  ID
                  Nombre
                  Autor
                  Año
                }
                @enduml
                """;

        UmlInterchangeService.ImportedClass imported = service.parse(plantUml.getBytes(StandardCharsets.UTF_8), "imagen.puml").classes().getFirst();

        assertThat(imported.attributes()).extracting(UmlInterchangeService.ImportedAttribute::name).containsExactly("ID", "Nombre", "Autor", "Año");
        assertThat(imported.attributes()).extracting(UmlInterchangeService.ImportedAttribute::dataType).containsOnly("String");
    }

    @Test
    void exportsPlantUmlWithPrimaryKeyAndManyToManyAssociationClass() {
        UUID projectId = UUID.randomUUID();
        UUID diagramId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID associationClassId = UUID.randomUUID();
        UmlClassResponse source = new UmlClassResponse(sourceId, diagramId, "Usuario", 120, 160, "#EAF3FF", 0L, List.of(new UmlAttributeResponse(UUID.randomUUID(), sourceId, "id", "UUID", "PRIVATE", true)), List.of());
        UmlClassResponse target = new UmlClassResponse(targetId, diagramId, "Rol", 420, 160, null, 0L, List.of(), List.of());
        UmlClassResponse associationClass = new UmlClassResponse(associationClassId, diagramId, "Asignación", 260, 260, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, sourceId, targetId, RelationType.ASSOCIATION, "tiene", null, null, null, null, associationClassId, List.of());

        String plantUml = new String(service.export(new DiagramDetailsResponse(new DiagramResponse(diagramId, projectId, "Dominio", 0L, null), List.of(source, target, associationClass), List.of(relation), List.of()), InterchangeFormat.PLANT_UML), StandardCharsets.UTF_8);

        assertThat(plantUml).contains("@startuml", "class \"Usuario\"", "- id : UUID <<PK>>", "\"1..*\" -- \"1..*\"", "(c_", "@enduml");
    }

    @Test
    void exportsEnterpriseArchitectXmiWithVisualClassDiagram() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID diagramId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID roleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UmlClassResponse user = new UmlClassResponse(userId, diagramId, "Usuario", 120, 160, "#EAF3FF", 0L, List.of(new UmlAttributeResponse(UUID.randomUUID(), userId, "correo", "String", "PRIVATE", true)), List.of());
        UmlClassResponse role = new UmlClassResponse(roleId, diagramId, "Rol", 420, 160, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, userId, roleId, RelationType.ASSOCIATION, "tiene", "1..1", "1..*", null, null, null, List.of());
        DiagramDetailsResponse details = new DiagramDetailsResponse(new DiagramResponse(diagramId, projectId, "Clases", 0L, null), List.of(user, role), List.of(relation), List.of());

        String xmi = new String(service.export(details, InterchangeFormat.EA_XMI), StandardCharsets.UTF_8);
        UmlInterchangeService.ImportedDiagram imported = service.parse(xmi.getBytes(StandardCharsets.UTF_8), "clases.xmi");

        assertThat(xmi).contains("xmi:version=\"2.1\"", "<xmi:Extension extender=\"Enterprise Architect\"", "<properties name=\"Clases\" type=\"Logical\"/>", "Left=120;Top=160", "<connectors>", "multiplicity=\"1..*\"", "subject=\"EAID_", "isID=\"true\"");
        assertThat(imported.classes()).hasSize(2);
        assertThat(imported.relations()).singleElement().extracting(UmlInterchangeService.ImportedRelation::type).isEqualTo(RelationType.ASSOCIATION);
    }

    @Test
    void exportsEnterpriseArchitectScriptWithDiagramAndConnectors() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID diagramId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sourceId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID targetId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UmlClassResponse source = new UmlClassResponse(sourceId, diagramId, "Usuario", 120, 160, null, 0L, List.of(new UmlAttributeResponse(UUID.randomUUID(), sourceId, "correo", "String", "PRIVATE", true)), List.of());
        UmlClassResponse target = new UmlClassResponse(targetId, diagramId, "Rol", 420, 160, null, 0L, List.of(), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, sourceId, targetId, RelationType.ASSOCIATION, "tiene", "1..1", "1..*", null, null, null, List.of());
        DiagramDetailsResponse details = new DiagramDetailsResponse(new DiagramResponse(diagramId, projectId, "Clases", 0L, null), List.of(source, target), List.of(relation), List.of());

        String script = new String(service.export(details, InterchangeFormat.EA_SCRIPT), StandardCharsets.UTF_8);

        assertThat(script).contains("Repository.GetTreeSelectedPackage()", "targetPackage.Elements.AddNew(name, \"Class\")", "diagram.DiagramObjects.AddNew", "source.Connectors.AddNew(name, type)", "connector.ClientEnd.Cardinality = sourceCardinality", "diagram.DiagramLinks.AddNew", "\"Usuario\"", "\"1..*\"");
    }
}
