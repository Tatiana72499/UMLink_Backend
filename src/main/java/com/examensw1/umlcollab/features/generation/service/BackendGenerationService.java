package com.examensw1.umlcollab.features.generation.service;

import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationParameterResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlOperationResponse;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackendGenerationService {
    private static final Set<String> JAVA_KEYWORDS = Set.of("class", "public", "private", "protected", "static", "void", "new", "return", "package", "import", "interface", "enum", "record", "extends", "implements", "long", "double", "boolean", "int", "float", "default", "null");
    private final DiagramService diagramService;

    public GeneratedBackend generate(UUID diagramId) {
        DiagramDetailsResponse details = diagramService.getDetails(diagramId);
        GenerationModel model = GenerationModel.from(details);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            add(zip, "README.md", readme(model));
            add(zip, "pom.xml", pom(model));
            add(zip, "src/main/resources/application.yml", applicationYaml(model));
            add(zip, "src/main/resources/db/migration/V1__initial_schema.sql", migration(model));
            add(zip, sourcePath(model, "GeneratedApplication.java"), applicationClass(model));
            add(zip, sourcePath(model, "config/CorsConfig.java"), corsConfig(model));
            add(zip, sourcePath(model, "common/ApiError.java"), apiError(model));
            add(zip, sourcePath(model, "common/ApiExceptionHandler.java"), apiExceptionHandler(model));
            for (ClassModel item : model.classes()) addClassFiles(zip, model, item);
            zip.finish();
            log.info("Backend generado para diagrama {}: {} clases, {} relaciones", diagramId, model.classes().size(), model.relations().size());
            return new GeneratedBackend(fileName(model), bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("No pudimos preparar el archivo del backend generado.", exception);
        }
    }

    private void addClassFiles(ZipOutputStream zip, GenerationModel model, ClassModel item) throws IOException {
        String feature = item.resourceName();
        add(zip, sourcePath(model, "features/" + feature + "/model/" + item.javaName() + ".java"), entity(model, item));
        add(zip, sourcePath(model, "features/" + feature + "/repository/" + item.javaName() + "Repository.java"), repository(model, item));
        add(zip, sourcePath(model, "features/" + feature + "/dto/Create" + item.javaName() + "Request.java"), request(model, item, "Create"));
        add(zip, sourcePath(model, "features/" + feature + "/dto/Update" + item.javaName() + "Request.java"), request(model, item, "Update"));
        add(zip, sourcePath(model, "features/" + feature + "/dto/" + item.javaName() + "Response.java"), response(model, item));
        for (OperationModel operation : item.operations()) if (!operation.parameters().isEmpty()) add(zip, sourcePath(model, "features/" + feature + "/dto/" + operation.requestClassName(item) + ".java"), operationRequest(model, item, operation));
        add(zip, sourcePath(model, "features/" + feature + "/service/" + item.javaName() + "Service.java"), service(model, item));
        add(zip, sourcePath(model, "features/" + feature + "/controller/" + item.javaName() + "Controller.java"), controller(model, item));
    }

    private void add(ZipOutputStream zip, String path, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String sourcePath(GenerationModel model, String file) { return "src/main/java/" + model.packageName().replace('.', '/') + "/" + file; }
    private String fileName(GenerationModel model) { return model.artifactName() + "-backend.zip"; }

    private String readme(GenerationModel model) {
        return "# " + model.applicationName() + " backend\n\nGenerado desde UMLink.\n\n## Ejecutar\n\n```powershell\nmvn spring-boot:run\n```\n\nConfigura `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Flyway crea el esquema inicial. Para una app web distinta de localhost, define `APP_CORS_ORIGIN_PATTERNS` con sus orígenes permitidos separados por coma.\n\n## Reglas aplicadas\n\n- Las clases sin PK reciben `UUID id` generado.\n- Los atributos sin tipo se generan como `String`.\n- Las asociaciones se traducen a relaciones JPA y claves foráneas o tablas intermedias según su cardinalidad.\n- Las dependencias no son persistentes; se conservan como comentario UML en esta generación inicial.\n- Las operaciones UML generan endpoints `POST` tipados y stubs en el service. Responden `501 Not Implemented` hasta que programes su regla de negocio.\n- Los errores REST usan el contrato uniforme `{timestamp, status, code, message, fieldErrors}`: validación `400`, referencia inválida `400`, duplicado `409` y no encontrado `404`.\n";
    }

    private String pom(GenerationModel model) {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.5.4</version></parent>
                  <groupId>%s</groupId><artifactId>%s-backend</artifactId><version>0.0.1-SNAPSHOT</version>
                  <properties><java.version>21</java.version></properties>
                  <dependencies>
                    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
                    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
                    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
                    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
                    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
                    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
                    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
                  </dependencies>
                  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
                </project>
                """.formatted(model.groupId(), model.artifactName());
    }

    private String applicationYaml(GenerationModel model) {
        return """
                spring:
                  application:
                    name: %s-backend
                  datasource:
                    url: ${DB_URL:jdbc:postgresql://localhost:5432/%s}
                    username: ${DB_USERNAME:postgres}
                    password: ${DB_PASSWORD:postgres}
                  jpa:
                    hibernate:
                      ddl-auto: validate
                    open-in-view: false
                  flyway:
                    enabled: true
                server:
                  port: ${PORT:8081}
                app:
                  cors:
                    allowed-origin-patterns: ${APP_CORS_ORIGIN_PATTERNS:http://localhost:*,http://127.0.0.1:*}
                """.formatted(model.artifactName(), model.artifactName().replace('-', '_'));
    }

    private String applicationClass(GenerationModel model) {
        return """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class GeneratedApplication {
                    public static void main(String[] args) { SpringApplication.run(GeneratedApplication.class, args); }
                }
                """.formatted(model.packageName());
    }

    private String corsConfig(GenerationModel model) {
        return """
                package %s.config;

                import org.springframework.beans.factory.annotation.Value;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.web.servlet.config.annotation.CorsRegistry;
                import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

                @Configuration
                public class CorsConfig implements WebMvcConfigurer {
                    @Value("${app.cors.allowed-origin-patterns}")
                    private String allowedOriginPatterns;

                    @Override
                    public void addCorsMappings(CorsRegistry registry) {
                        registry.addMapping("/api/**")
                                .allowedOriginPatterns(allowedOriginPatterns.split(","))
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("Content-Type", "Accept");
                    }
                }
                """.formatted(model.packageName());
    }

    private String apiError(GenerationModel model) {
        return "package " + model.packageName() + ".common;\n\nimport java.time.Instant;\nimport java.util.Map;\n\npublic record ApiError(Instant timestamp, int status, String code, String message, Map<String, String> fieldErrors) {}\n";
    }

    private String apiExceptionHandler(GenerationModel model) {
        return "package " + model.packageName() + ".common;\n\nimport jakarta.servlet.http.HttpServletRequest;\nimport java.time.Instant;\nimport java.util.LinkedHashMap;\nimport java.util.Map;\nimport org.springframework.http.HttpStatus;\nimport org.springframework.http.ResponseEntity;\nimport org.springframework.validation.FieldError;\nimport org.springframework.web.bind.MethodArgumentNotValidException;\nimport org.springframework.web.bind.annotation.ExceptionHandler;\nimport org.springframework.web.bind.annotation.RestControllerAdvice;\nimport org.springframework.web.server.ResponseStatusException;\n\n@RestControllerAdvice public class ApiExceptionHandler {\n    @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) { var fields = new LinkedHashMap<String, String>(); for (FieldError error : exception.getBindingResult().getFieldErrors()) fields.put(error.getField(), error.getDefaultMessage()); return response(HttpStatus.BAD_REQUEST, \"VALIDATION_ERROR\", \"Revisa los campos enviados.\", fields); }\n    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<ApiError> status(ResponseStatusException exception) { var status = HttpStatus.valueOf(exception.getStatusCode().value()); String code = status == HttpStatus.NOT_FOUND ? \"NOT_FOUND\" : status == HttpStatus.CONFLICT ? \"CONFLICT\" : status == HttpStatus.NOT_IMPLEMENTED ? \"NOT_IMPLEMENTED\" : \"REQUEST_ERROR\"; return response(status, code, exception.getReason() == null ? \"No pudimos completar la solicitud.\" : exception.getReason(), Map.of()); }\n    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message, Map<String, String> fields) { return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, fields)); }\n}\n";
    }

    private String migration(GenerationModel model) {
        StringBuilder sql = new StringBuilder("-- Generado por UMLink. Revisa nombres, nulabilidad y reglas de negocio antes de producción.\n\n");
        for (ClassModel item : model.classes()) {
            sql.append("CREATE TABLE ").append(item.tableName()).append(" (\n");
            List<String> columns = new ArrayList<>();
            columns.add(item.idColumnSql());
            for (AttributeModel attribute : item.attributes()) columns.add("    " + attribute.columnName() + " " + attribute.sqlType());
            for (RelationModel relation : model.ownedRelations(item)) if (relation.storage() != Storage.JOIN_TABLE && relation.storage() != Storage.TARGET_FOREIGN_KEY) columns.add("    " + relation.columnName() + " " + relation.target().idSqlType() + (relation.storage() == Storage.ONE_TO_ONE ? " UNIQUE" : ""));
            sql.append(String.join(",\n", columns)).append("\n);\n\n");
        }
        for (RelationModel relation : model.relations()) {
            if (relation.storage() == Storage.SOURCE_FOREIGN_KEY || relation.storage() == Storage.ONE_TO_ONE) {
                sql.append("ALTER TABLE ").append(relation.source().tableName()).append(" ADD CONSTRAINT fk_").append(relation.source().tableName()).append("_").append(relation.target().tableName()).append(" FOREIGN KEY (").append(relation.columnName()).append(") REFERENCES ").append(relation.target().tableName()).append("(id);\n");
            } else if (relation.storage() == Storage.TARGET_FOREIGN_KEY) {
                sql.append("ALTER TABLE ").append(relation.target().tableName()).append(" ADD COLUMN ").append(relation.columnName()).append(" ").append(relation.source().idSqlType()).append(";\n");
                sql.append("ALTER TABLE ").append(relation.target().tableName()).append(" ADD CONSTRAINT fk_").append(relation.target().tableName()).append("_").append(relation.source().tableName()).append(" FOREIGN KEY (").append(relation.columnName()).append(") REFERENCES ").append(relation.source().tableName()).append("(id);\n");
            } else if (relation.storage() == Storage.JOIN_TABLE) {
                sql.append("CREATE TABLE ").append(relation.joinTable()).append(" (\n    ").append(relation.source().tableName()).append("_id ").append(relation.source().idSqlType()).append(" NOT NULL REFERENCES ").append(relation.source().tableName()).append("(id),\n    ").append(relation.target().tableName()).append("_id ").append(relation.target().idSqlType()).append(" NOT NULL REFERENCES ").append(relation.target().tableName()).append("(id),\n    PRIMARY KEY (").append(relation.source().tableName()).append("_id, ").append(relation.target().tableName()).append("_id)\n);\n");
            }
        }
        return sql.toString();
    }

    private String entity(GenerationModel model, ClassModel item) {
        StringBuilder source = new StringBuilder("package ").append(model.packageName()).append(".features.").append(item.resourceName()).append(".model;\n\nimport jakarta.persistence.*;\nimport java.util.*;\nimport java.time.*;\n");
        for (RelationModel relation : model.ownedRelations(item)) source.append("import ").append(model.packageName()).append(".features.").append(relation.target().resourceName()).append(".model.").append(relation.target().javaName()).append(";\n");
        source.append("\n@Entity\n@Table(name = \"").append(item.tableName()).append("\")\npublic class ").append(item.javaName()).append(" {\n");
        source.append(item.idJava());
        for (AttributeModel attribute : item.attributes()) source.append("    @Column(name = \"").append(attribute.columnName()).append("\")\n    private ").append(attribute.javaType()).append(" ").append(attribute.fieldName()).append(";\n\n");
        for (RelationModel relation : model.ownedRelations(item)) source.append(relation.entityField(item));
        source.append("    public ").append(item.idType()).append(" getId() { return id; }\n    public void setId(").append(item.idType()).append(" id) { this.id = id; }\n");
        for (AttributeModel attribute : item.attributes()) source.append("    public ").append(attribute.javaType()).append(" get").append(capitalize(attribute.fieldName())).append("() { return ").append(attribute.fieldName()).append("; }\n    public void set").append(capitalize(attribute.fieldName())).append("(").append(attribute.javaType()).append(" value) { this.").append(attribute.fieldName()).append(" = value; }\n");
        for (RelationModel relation : model.ownedRelations(item)) source.append(relation.accessors());
        return source.append("}\n").toString();
    }

    private String repository(GenerationModel model, ClassModel item) { return "package " + model.packageName() + ".features." + item.resourceName() + ".repository;\n\nimport " + model.packageName() + ".features." + item.resourceName() + ".model." + item.javaName() + ";\nimport org.springframework.data.jpa.repository.JpaRepository;\nimport java.util.UUID;\n\npublic interface " + item.javaName() + "Repository extends JpaRepository<" + item.javaName() + ", " + item.idType() + "> {}\n"; }

    private String request(GenerationModel model, ClassModel item, String prefix) {
        String parameters = item.attributes().stream().map(attribute -> attribute.javaType() + " " + attribute.fieldName()).reduce((left, right) -> left + ", " + right).orElse("");
        boolean validateManualId = "Create".equals(prefix) && !item.generatedId();
        if (validateManualId) {
            String idParameter = "String".equals(item.idType()) ? "@NotBlank(message = \"El identificador es obligatorio.\") String id" : "@NotNull(message = \"El identificador es obligatorio.\") " + item.idType() + " id";
            parameters = idParameter + (parameters.isBlank() ? "" : ", " + parameters);
        }
        String relationParameters = model.ownedRelations(item).stream().map(RelationModel::requestParameter).reduce("", (left, right) -> left + ", " + right);
        String imports = "import java.util.UUID;\nimport java.time.*;\n" + (model.ownedRelations(item).stream().anyMatch(RelationModel::isCollection) ? "import java.util.List;\n" : "") + (validateManualId ? "import jakarta.validation.constraints." + ("String".equals(item.idType()) ? "NotBlank" : "NotNull") + ";\n" : "");
        return "package " + model.packageName() + ".features." + item.resourceName() + ".dto;\n\n" + imports + "public record " + prefix + item.javaName() + "Request(" + parameters + relationParameters + ") {}\n";
    }

    private String response(GenerationModel model, ClassModel item) {
        String fields = item.idType() + " id" + item.attributes().stream().map(attribute -> attribute.javaType() + " " + attribute.fieldName()).reduce("", (left, right) -> left + ", " + right) + model.ownedRelations(item).stream().map(RelationModel::responseParameter).reduce("", (left, right) -> left + ", " + right);
        String imports = "import java.util.UUID;\nimport java.time.*;\n" + (model.ownedRelations(item).stream().anyMatch(RelationModel::isCollection) ? "import java.util.List;\n" : "");
        return "package " + model.packageName() + ".features." + item.resourceName() + ".dto;\n\n" + imports + "public record " + item.javaName() + "Response(" + fields + ") {}\n";
    }

    private String operationRequest(GenerationModel model, ClassModel item, OperationModel operation) {
        String parameters = operation.parameters().stream().map(parameter -> parameter.validation() + " " + parameter.javaType() + " " + parameter.fieldName()).reduce((left, right) -> left + ", " + right).orElse("");
        return "package " + model.packageName() + ".features." + item.resourceName() + ".dto;\n\nimport jakarta.validation.constraints.*;\nimport java.time.*;\nimport java.util.*;\n\npublic record " + operation.requestClassName(item) + "(" + parameters + ") {}\n";
    }

    private String operationServiceMethods(ClassModel item) {
        return item.operations().stream().map(operation -> {
            String parameters = operation.parameters().stream().map(parameter -> parameter.javaType() + " " + parameter.fieldName()).reduce("", (left, right) -> left + ", " + right);
            return "    public " + operation.returnType() + " " + operation.methodName() + "(" + item.idType() + " id" + parameters + ") { findEntity(id); throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, \"La operación " + operation.name() + " debe implementar su regla de negocio.\"); }\n";
        }).reduce("", String::concat);
    }

    private String operationControllerMethods(ClassModel item) {
        return item.operations().stream().map(operation -> {
            String request = operation.parameters().isEmpty() ? "" : ", @Valid @RequestBody " + operation.requestClassName(item) + " request";
            String arguments = operation.parameters().isEmpty() ? "id" : "id" + operation.parameters().stream().map(parameter -> "request." + parameter.fieldName() + "()").reduce("", (left, right) -> left + ", " + right);
            String invocation = "service." + operation.methodName() + "(" + arguments + ")";
            String body = "void".equals(operation.returnType()) ? invocation + ";" : "return " + invocation + ";";
            return "    @PostMapping(\"/{id}/operations/" + operation.path() + "\") public " + operation.returnType() + " " + operation.methodName() + "(@PathVariable " + item.idType() + " id" + request + ") { " + body + " }\n";
        }).reduce("", String::concat);
    }

    private String service(GenerationModel model, ClassModel item) {
        String packageName = model.packageName() + ".features." + item.resourceName();
        String assignments = item.attributes().stream().map(attribute -> "entity.set" + capitalize(attribute.fieldName()) + "(request." + attribute.fieldName() + "());").reduce("", (left, right) -> left + right) + model.ownedRelations(item).stream().map(RelationModel::assignment).reduce("", (left, right) -> left + right);
        String responseFields = "entity.getId()" + item.attributes().stream().map(attribute -> "entity.get" + capitalize(attribute.fieldName()) + "()").reduce("", (left, right) -> left + ", " + right) + model.ownedRelations(item).stream().map(RelationModel::responseValue).reduce("", (left, right) -> left + ", " + right);
        String relationImports = model.ownedRelations(item).stream().map(relation -> "import " + model.packageName() + ".features." + relation.target().resourceName() + ".model." + relation.target().javaName() + ";\nimport " + model.packageName() + ".features." + relation.target().resourceName() + ".repository." + relation.target().javaName() + "Repository;\n").reduce("", String::concat);
        String relationFields = model.ownedRelations(item).stream().map(relation -> "    private final " + relation.target().javaName() + "Repository " + relation.repositoryField() + ";\n").reduce("", String::concat);
        String createIdAssignment = item.generatedId() ? "" : "if (repository.existsById(request.id())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, \"Ya existe un registro con ese identificador.\"); entity.setId(request.id()); ";
        return "package " + packageName + ".service;\n\nimport " + packageName + ".dto.*;\nimport " + packageName + ".model." + item.javaName() + ";\nimport " + packageName + ".repository." + item.javaName() + "Repository;\n" + relationImports + "import java.util.*;\nimport " + item.idImport() + ";\nimport lombok.RequiredArgsConstructor;\nimport org.springframework.http.HttpStatus;\nimport org.springframework.stereotype.Service;\nimport org.springframework.transaction.annotation.Transactional;\nimport org.springframework.web.server.ResponseStatusException;\n\n@Service @RequiredArgsConstructor public class " + item.javaName() + "Service {\n    private final " + item.javaName() + "Repository repository;\n" + relationFields + "    @Transactional(readOnly = true) public List<" + item.javaName() + "Response> findAll() { return repository.findAll().stream().map(this::toResponse).toList(); }\n    @Transactional(readOnly = true) public " + item.javaName() + "Response findById(" + item.idType() + " id) { return toResponse(findEntity(id)); }\n    public " + item.javaName() + "Response create(Create" + item.javaName() + "Request request) { " + item.javaName() + " entity = new " + item.javaName() + "(); " + createIdAssignment + assignments + " return toResponse(repository.save(entity)); }\n    public " + item.javaName() + "Response update(" + item.idType() + " id, Update" + item.javaName() + "Request request) { " + item.javaName() + " entity = findEntity(id); " + assignments + " return toResponse(repository.save(entity)); }\n    public void delete(" + item.idType() + " id) { repository.delete(findEntity(id)); }\n" + operationServiceMethods(item) + "    private " + item.javaName() + " findEntity(" + item.idType() + " id) { return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, \"" + item.javaName() + " no encontrado.\")); }\n    private " + item.javaName() + "Response toResponse(" + item.javaName() + " entity) { return new " + item.javaName() + "Response(" + responseFields + "); }\n}\n";
    }

    private String controller(GenerationModel model, ClassModel item) {
        String packageName = model.packageName() + ".features." + item.resourceName();
        return "package " + packageName + ".controller;\n\nimport " + packageName + ".dto.*;\nimport " + packageName + ".service." + item.javaName() + "Service;\nimport " + item.idImport() + ";\nimport java.util.List;\nimport jakarta.validation.Valid;\nimport lombok.RequiredArgsConstructor;\nimport org.springframework.http.HttpStatus;\nimport org.springframework.web.bind.annotation.*;\n\n@RestController @RequestMapping(\"/api/" + item.resourceName() + "\") @RequiredArgsConstructor public class " + item.javaName() + "Controller {\n    private final " + item.javaName() + "Service service;\n    @GetMapping public List<" + item.javaName() + "Response> findAll() { return service.findAll(); }\n    @GetMapping(\"/{id}\") public " + item.javaName() + "Response findById(@PathVariable " + item.idType() + " id) { return service.findById(id); }\n    @PostMapping @ResponseStatus(HttpStatus.CREATED) public " + item.javaName() + "Response create(@Valid @RequestBody Create" + item.javaName() + "Request request) { return service.create(request); }\n    @PutMapping(\"/{id}\") public " + item.javaName() + "Response update(@PathVariable " + item.idType() + " id, @Valid @RequestBody Update" + item.javaName() + "Request request) { return service.update(id, request); }\n    @DeleteMapping(\"/{id}\") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable " + item.idType() + " id) { service.delete(id); }\n" + operationControllerMethods(item) + "}\n";
    }

    private String capitalize(String value) { return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }

    public record GeneratedBackend(String fileName, byte[] content) {}

    private enum Storage { SOURCE_FOREIGN_KEY, TARGET_FOREIGN_KEY, ONE_TO_ONE, JOIN_TABLE, NONE }

    private record AttributeModel(String fieldName, String columnName, String javaType, String sqlType) {}
    private record OperationParameterModel(String fieldName, String javaType) {
        String validation() { return "String".equals(javaType) ? "@NotBlank(message = \"El campo es obligatorio.\")" : "@NotNull(message = \"El campo es obligatorio.\")"; }
    }
    private record OperationModel(String name, String methodName, String path, String returnType, List<OperationParameterModel> parameters) {
        String requestClassName(ClassModel item) { return "Execute" + capitalizeStatic(methodName) + item.javaName() + "Request"; }
        private static String capitalizeStatic(String value) { return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }
    }
    private record ClassModel(UUID id, String javaName, String resourceName, String tableName, String idType, String idSqlType, boolean generatedId, List<AttributeModel> attributes, List<OperationModel> operations) {
        String idColumnSql() { return "    id " + idSqlType + " PRIMARY KEY"; }
        String idJava() { return generatedId ? "    @Id\n    @GeneratedValue(strategy = GenerationType.UUID)\n    private UUID id;\n\n" : "    @Id\n    @Column(name = \"id\")\n    private " + idType + " id;\n\n"; }
        String idImport() { return "java.util.UUID"; }
    }
    private record RelationModel(ClassModel source, ClassModel target, Storage storage) {
        String columnName() { return storage == Storage.TARGET_FOREIGN_KEY ? source.tableName() + "_id" : target.tableName() + "_id"; }
        String joinTable() { return source.tableName() + "_" + target.tableName(); }
        String fieldName() { return target.resourceName(); }
        String accessorName() { return capitalizeStatic(fieldName()) + (isCollection() ? (storage == Storage.JOIN_TABLE ? "Set" : "List") : ""); }
        String requestName() { return fieldName() + (isCollection() ? "Ids" : "Id"); }
        String repositoryField() { return Character.toLowerCase(target.javaName().charAt(0)) + target.javaName().substring(1) + "Repository"; }
        boolean isCollection() { return storage == Storage.TARGET_FOREIGN_KEY || storage == Storage.JOIN_TABLE; }
        String requestParameter() { return (isCollection() ? "List<" : "") + target.idType() + (isCollection() ? "> " : " ") + requestName(); }
        String responseParameter() { return (isCollection() ? "List<" : "") + target.idType() + (isCollection() ? "> " : " ") + requestName(); }
        String responseValue() { return isCollection() ? "entity.get" + accessorName() + "().stream().map(item -> item.getId()).toList()" : "entity.get" + accessorName() + "() == null ? null : entity.get" + accessorName() + "().getId()"; }
        String assignment() {
            if (!isCollection()) return "if (request." + requestName() + "() == null) { entity.set" + accessorName() + "(null); } else { entity.set" + accessorName() + "(" + repositoryField() + ".findById(request." + requestName() + "()).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, \"Referencia " + target.javaName() + " no encontrada.\"))); }";
            String ids = "(request." + requestName() + "() == null ? List.<" + target.idType() + ">of() : request." + requestName() + "())";
            return "List<" + target.javaName() + "> " + fieldName() + "Values = " + repositoryField() + ".findAllById(" + ids + "); if (" + fieldName() + "Values.size() != " + ids + ".size()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, \"Una o más referencias " + target.javaName() + " no existen.\"); entity.set" + accessorName() + "(new " + (storage == Storage.JOIN_TABLE ? "LinkedHashSet" : "ArrayList") + "<>(" + fieldName() + "Values));";
        }
        String entityField(ClassModel owner) {
            String targetType = target.javaName();
            String field = target.resourceName();
            if (storage == Storage.SOURCE_FOREIGN_KEY && owner == source) return "    @ManyToOne\n    @JoinColumn(name = \"" + columnName() + "\")\n    private " + targetType + " " + field + ";\n\n";
            if (storage == Storage.ONE_TO_ONE && owner == source) return "    @OneToOne\n    @JoinColumn(name = \"" + columnName() + "\", unique = true)\n    private " + targetType + " " + field + ";\n\n";
            if (storage == Storage.TARGET_FOREIGN_KEY && owner == source) return "    @OneToMany\n    @JoinColumn(name = \"" + columnName() + "\")\n    private List<" + targetType + "> " + field + "List = new ArrayList<>();\n\n";
            if (storage == Storage.JOIN_TABLE && owner == source) return "    @ManyToMany\n    @JoinTable(name = \"" + joinTable() + "\", joinColumns = @JoinColumn(name = \"" + source.tableName() + "_id\"), inverseJoinColumns = @JoinColumn(name = \"" + target.tableName() + "_id\"))\n    private Set<" + targetType + "> " + field + "Set = new LinkedHashSet<>();\n\n";
            return "";
        }
        String accessors() {
            String type = isCollection() ? (storage == Storage.JOIN_TABLE ? "Set<" : "List<") + target.javaName() + ">" : target.javaName();
            return "    public " + type + " get" + accessorName() + "() { return " + fieldName() + (isCollection() ? (storage == Storage.JOIN_TABLE ? "Set" : "List") : "") + "; }\n    public void set" + accessorName() + "(" + type + " value) { this." + fieldName() + (isCollection() ? (storage == Storage.JOIN_TABLE ? "Set" : "List") : "") + " = value; }\n";
        }
        private static String capitalizeStatic(String value) { return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1); }
    }
    private record GenerationModel(String applicationName, String artifactName, String groupId, String packageName, List<ClassModel> classes, List<RelationModel> relations) {
        static GenerationModel from(DiagramDetailsResponse details) {
            if (details.classes().isEmpty()) throw new IllegalArgumentException("El diagrama debe tener al menos una clase para generar un backend.");
            String artifact = slug(details.diagram().name());
            String packageSegment = artifact.replace('-', '_').toLowerCase(Locale.ROOT);
            Map<UUID, ClassModel> classesById = new LinkedHashMap<>();
            Set<String> names = new java.util.HashSet<>();
            for (UmlClassResponse item : details.classes().stream().sorted(Comparator.comparing(UmlClassResponse::name)).toList()) {
                String javaName = className(item.name());
                if (!names.add(javaName.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Dos clases producen el mismo nombre Java: " + javaName + ". Renómbralas antes de generar.");
                UmlAttributeResponse primaryKey = item.attributes().stream().filter(UmlAttributeResponse::primaryKey).findFirst()
                        .or(() -> item.attributes().stream().filter(attribute -> "id".equals(fieldName(attribute.name()))).findFirst()).orElse(null);
                List<AttributeModel> attributes = item.attributes().stream().filter(attribute -> attribute != primaryKey).map(GenerationModel::attribute).toList();
                List<OperationModel> operations = item.operations().stream().map(GenerationModel::operation).toList();
                Set<String> operationPaths = new java.util.HashSet<>();
                for (OperationModel operation : operations) if (!operationPaths.add(operation.path())) throw new IllegalArgumentException("Dos operaciones de " + javaName + " producen el mismo endpoint: " + operation.path() + ". Renómbralas antes de generar.");
                boolean generatedId = primaryKey == null;
                String idType = generatedId ? "UUID" : javaType(primaryKey.dataType());
                String idSqlType = generatedId ? "UUID" : sqlType(primaryKey.dataType());
                classesById.put(item.id(), new ClassModel(item.id(), javaName, resourceName(javaName), snake(javaName), idType, idSqlType, generatedId, attributes, operations));
            }
            List<RelationModel> relations = new ArrayList<>();
            for (UmlRelationResponse relation : details.relations()) {
                if (relation.type() == RelationType.DEPENDENCY || relation.type() == RelationType.GENERALIZATION || relation.type() == RelationType.REALIZATION) continue;
                ClassModel source = classesById.get(relation.sourceClassId()); ClassModel target = classesById.get(relation.targetClassId());
                if (source == null || target == null || source == target) continue;
                boolean sourceMany = ("1..*".equals(relation.sourceCardinality()) || "0..*".equals(relation.sourceCardinality())); boolean targetMany = ("1..*".equals(relation.targetCardinality()) || "0..*".equals(relation.targetCardinality()));
                Storage storage = sourceMany && targetMany ? Storage.JOIN_TABLE : sourceMany ? Storage.SOURCE_FOREIGN_KEY : targetMany ? Storage.TARGET_FOREIGN_KEY : Storage.ONE_TO_ONE;
                relations.add(new RelationModel(source, target, storage));
            }
            return new GenerationModel(details.diagram().name(), artifact, "com.generated", "com.generated." + packageSegment, List.copyOf(classesById.values()), List.copyOf(relations));
        }
        List<RelationModel> ownedRelations(ClassModel owner) { return relations.stream().filter(relation -> relation.source() == owner).toList(); }
        private static AttributeModel attribute(UmlAttributeResponse item) { return new AttributeModel(fieldName(item.name()), snake(fieldName(item.name())), javaType(item.dataType()), sqlType(item.dataType())); }
        private static OperationModel operation(UmlOperationResponse item) {
            String methodName = fieldName(item.name());
            if (JAVA_KEYWORDS.contains(methodName.toLowerCase(Locale.ROOT))) methodName += "Operation";
            Set<String> parameterNames = new java.util.HashSet<>();
            List<OperationParameterModel> parameters = item.parameters().stream().map(parameter -> new OperationParameterModel(operationParameterName(parameter.name()), javaType(parameter.dataType()))).toList();
            for (OperationParameterModel parameter : parameters) if (!parameterNames.add(parameter.fieldName())) throw new IllegalArgumentException("Dos parámetros de " + item.name() + " producen el mismo nombre Java: " + parameter.fieldName() + ".");
            return new OperationModel(item.name(), methodName, slug(item.name()), "VOID".equals(item.returnType()) ? "void" : javaType(item.returnType()), parameters);
        }
        private static String operationParameterName(String value) { String result = fieldName(value); return JAVA_KEYWORDS.contains(result.toLowerCase(Locale.ROOT)) ? result + "Value" : result; }
        private static String javaType(String type) { return switch (type) { case "Integer" -> "Integer"; case "Long" -> "Long"; case "Double" -> "Double"; case "Boolean" -> "Boolean"; case "UUID" -> "UUID"; case "LocalDate" -> "LocalDate"; case "LocalDateTime" -> "LocalDateTime"; default -> "String"; }; }
        private static String sqlType(String type) { return switch (javaType(type)) { case "Integer" -> "INTEGER"; case "Long" -> "BIGINT"; case "Double" -> "DOUBLE PRECISION"; case "Boolean" -> "BOOLEAN"; case "UUID" -> "UUID"; case "LocalDate" -> "DATE"; case "LocalDateTime" -> "TIMESTAMP"; default -> "VARCHAR(255)"; }; }
        private static String className(String value) { String result = camel(value); if (result.isBlank()) throw new IllegalArgumentException("Una clase no tiene un nombre compatible con Java."); return JAVA_KEYWORDS.contains(result.toLowerCase(Locale.ROOT)) ? result + "Entity" : result; }
        private static String fieldName(String value) { String result = camel(value); if (result.isBlank()) throw new IllegalArgumentException("Un atributo no tiene un nombre compatible con Java."); return Character.toLowerCase(result.charAt(0)) + result.substring(1); }
        private static String resourceName(String value) { String result = Character.toLowerCase(value.charAt(0)) + value.substring(1); return result.endsWith("s") ? result.toLowerCase(Locale.ROOT) : result.toLowerCase(Locale.ROOT) + "s"; }
        private static String slug(String value) { String result = ascii(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); return result.isBlank() ? "generated-model" : result; }
        private static String snake(String value) { return ascii(value).replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9]+", "_").replaceAll("(^_|_$)", "").toLowerCase(Locale.ROOT); }
        private static String camel(String value) { String cleaned = ascii(value).replaceAll("[^A-Za-z0-9]+", " ").trim(); StringBuilder result = new StringBuilder(); for (String word : cleaned.split("\\s+")) if (!word.isBlank()) result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1)); return result.toString(); }
        private static String ascii(String value) { return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }
    }
}
