package com.examensw1.umlcollab.features.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationParameterResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationResponse;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackendGenerationServiceTest {
    @Mock private DiagramService diagramService;

    @Test
    void generatesLayeredSpringBootProjectAndPostgresMigration() throws IOException {
        UUID diagramId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UmlClassResponse book = new UmlClassResponse(bookId, diagramId, "Libro", 0, 0, null, 0L,
                List.of(new UmlAttributeResponse(UUID.randomUUID(), bookId, "titulo", "String", "PRIVATE", false)), List.of(new UmlOperationResponse(UUID.randomUUID(), bookId, "prestar", "PUBLIC", "Boolean", List.of(new UmlOperationParameterResponse(UUID.randomUUID(), "dias", "Integer", 0)))));
        UmlClassResponse student = new UmlClassResponse(studentId, diagramId, "Estudiante", 0, 0, null, 0L,
                List.of(new UmlAttributeResponse(UUID.randomUUID(), studentId, "ci", "Integer", "PRIVATE", true)), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, bookId, studentId, RelationType.ASSOCIATION, null, "1..1", "1..*");
        when(diagramService.getDetails(diagramId)).thenReturn(new DiagramDetailsResponse(
                new DiagramResponse(diagramId, projectId, "Biblioteca", 0L, Instant.now()), List.of(book, student), List.of(relation), List.of()));

        BackendGenerationService.GeneratedBackend generated = new BackendGenerationService(diagramService).generate(diagramId);
        Map<String, String> entries = unzip(generated.content());

        assertThat(generated.fileName()).isEqualTo("biblioteca-backend.zip");
        assertThat(entries).containsKeys("pom.xml", "src/main/resources/db/migration/V1__initial_schema.sql", "src/main/java/com/generated/biblioteca/GeneratedApplication.java", "src/main/java/com/generated/biblioteca/config/CorsConfig.java", "src/main/java/com/generated/biblioteca/common/ApiError.java", "src/main/java/com/generated/biblioteca/common/ApiExceptionHandler.java");
        assertThat(entries.get("pom.xml")).contains("org.projectlombok", "<java.version>21</java.version>");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/config/CorsConfig.java")).contains("allowedOriginPatterns", "addMapping(\"/api/**\")", "allowedMethods(\"GET\", \"POST\", \"PUT\", \"DELETE\", \"OPTIONS\")");
        assertThat(entries.get("src/main/resources/db/migration/V1__initial_schema.sql")).contains("CREATE TABLE libro", "CREATE TABLE estudiante", "titulo VARCHAR(255)", "id INTEGER PRIMARY KEY", "ADD COLUMN libro_id UUID");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/model/Libro.java"))
                .contains("@OneToMany", "private List<Estudiante> estudiantesList", "private String titulo", "getEstudiantesList", "setEstudiantesList");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/dto/CreateLibroRequest.java"))
                .contains("List<Integer> estudiantesIds");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/dto/LibroResponse.java"))
                .contains("List<Integer> estudiantesIds");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/service/LibroService.java"))
                .contains("import com.generated.biblioteca.features.estudiantes.model.Estudiante;", "EstudianteRepository estudianteRepository", "org.springframework.transaction.annotation.Transactional", "@Transactional(readOnly = true) public List<LibroResponse> findAll()", "findAllById", "Una o más referencias Estudiante no existen", "setEstudiantesList", "(request.estudiantesIds() == null ? List.<Integer>of() : request.estudiantesIds()).size()");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/estudiantes/dto/CreateEstudianteRequest.java"))
                .contains("import java.util.UUID;", "NotNull", "Integer id");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/estudiantes/service/EstudianteService.java"))
                .contains("entity.setId(request.id())");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/estudiantes/controller/EstudianteController.java"))
                .contains("jakarta.validation.Valid", "@Valid @RequestBody");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/common/ApiExceptionHandler.java"))
                .contains("VALIDATION_ERROR", "NOT_FOUND", "CONFLICT", "MethodArgumentNotValidException");
    }

    @Test
    void generatesTypedStubForUmlOperation() throws IOException {
        UUID diagramId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UmlOperationResponse operation = new UmlOperationResponse(UUID.randomUUID(), classId, "prestar", "PUBLIC", "Boolean", List.of(new UmlOperationParameterResponse(UUID.randomUUID(), "dias", "Integer", 0)));
        UmlClassResponse book = new UmlClassResponse(classId, diagramId, "Libro", 0, 0, null, 0L, List.of(), List.of(operation));
        when(diagramService.getDetails(diagramId)).thenReturn(new DiagramDetailsResponse(new DiagramResponse(diagramId, UUID.randomUUID(), "Biblioteca", 0L, Instant.now()), List.of(book), List.of(), List.of()));

        Map<String, String> entries = unzip(new BackendGenerationService(diagramService).generate(diagramId).content());

        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/dto/ExecutePrestarLibroRequest.java")).contains("@NotNull", "Integer dias");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/service/LibroService.java")).contains("public Boolean prestar(UUID id, Integer dias)", "HttpStatus.NOT_IMPLEMENTED");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/features/libros/controller/LibroController.java")).contains("@PostMapping(\"/{id}/operations/prestar\")", "@Valid @RequestBody ExecutePrestarLibroRequest request", "return service.prestar(id, request.dias());");
        assertThat(entries.get("src/main/java/com/generated/biblioteca/common/ApiExceptionHandler.java")).contains("NOT_IMPLEMENTED");
    }
    private Map<String, String> unzip(byte[] content) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        return entries;
    }
}
