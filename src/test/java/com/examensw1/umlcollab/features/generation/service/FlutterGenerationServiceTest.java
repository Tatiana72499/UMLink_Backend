package com.examensw1.umlcollab.features.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.DiagramResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
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
class FlutterGenerationServiceTest {
    @Mock private DiagramService diagramService;

    @Test
    void generatesFlutterCrudProjectWithOptionalLocalAiConfiguration() throws IOException {
        UUID diagramId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UmlClassResponse book = new UmlClassResponse(bookId, diagramId, "Libro", 0, 0, null, 0L,
                List.of(new UmlAttributeResponse(UUID.randomUUID(), bookId, "titulo", "String", "PRIVATE", false)), List.of());
        UmlClassResponse student = new UmlClassResponse(studentId, diagramId, "Estudiante", 0, 0, null, 0L,
                List.of(new UmlAttributeResponse(UUID.randomUUID(), studentId, "ci", "Integer", "PRIVATE", true), new UmlAttributeResponse(UUID.randomUUID(), studentId, "activo", "Boolean", "PRIVATE", false)), List.of());
        UmlRelationResponse relation = new UmlRelationResponse(UUID.randomUUID(), diagramId, bookId, studentId, RelationType.ASSOCIATION, null, "1..1", "1..*");
        when(diagramService.getDetails(diagramId)).thenReturn(new DiagramDetailsResponse(
                new DiagramResponse(diagramId, projectId, "Biblioteca", 0L, Instant.now()), List.of(book, student), List.of(relation), List.of()));

        FlutterGenerationService.GeneratedFlutter generated = new FlutterGenerationService(diagramService).generate(diagramId);
        Map<String, String> entries = unzip(generated.content());

        assertThat(generated.fileName()).isEqualTo("biblioteca-flutter.zip");
        assertThat(entries).containsKeys("pubspec.yaml", "lib/main.dart", "lib/core/api_client.dart", "lib/core/app_theme.dart", "lib/core/id_generator.dart", "lib/core/offline_sync_service.dart", "lib/core/ollama_config.dart", "lib/ai/assistant_schema.dart", "lib/ai/ollama_assistant_service.dart", "lib/ai/assistant_sheet.dart", "lib/ai/voice_service.dart", "lib/ai/voice_service_web.dart", "lib/ai/voice_service_vosk.dart", "lib/shared/assistant_button.dart", "lib/shared/app_empty_state.dart", "lib/shared/offline_status_banner.dart", "lib/shared/relation_selector.dart", "lib/shared/server_settings_button.dart",
                "lib/features/libros/models/libro.dart", "lib/features/libros/data-access/libro_api.dart", "lib/features/libros/pages/libro_page.dart");
        assertThat(entries.get("lib/features/libros/models/libro.dart")).contains("class Libro", "final String? titulo", "final List<String>? estudiantesIds", "toRequestJson", "Libro(id: json['id']?.toString(), titulo:");
        assertThat(entries.get("lib/features/libros/pages/libro_page.dart")).contains("RelationSelector", "path: '/estudiantes'", "selectedEstudiantesIds", "Libro(id: value?.id, titulo:").doesNotContain("id: value?.id, ,");
        assertThat(entries.get("lib/features/estudiantes/models/estudiante.dart")).contains("final bool? activo", "final String? id", "Estudiante(id: json['id']?.toString(), activo:", "'id': id, 'activo': activo").doesNotContain(", ,");
        assertThat(entries.get("lib/features/estudiantes/pages/estudiante_page.dart")).contains("idController", "Identificador");
        assertThat(entries.get("pubspec.yaml")).contains("http: ^0.13.6", "shared_preferences: ^2.3.2", "vosk_flutter: ^0.3.48", "permission_handler: ^12.0.3", "web: ^1.1.1", "assets/models/");
        assertThat(entries.get("lib/core/api_client.dart")).contains("AppConfig.apiBaseUrl", "package:http/http.dart", "http.Client", "IdGenerator.v4", "ApiException", "NetworkUnavailableException", "OfflineSyncService.enqueue", "_errorMessage").doesNotContain("dart:io");
        assertThat(entries.get("lib/core/id_generator.dart")).contains("Random.secure", "static String v4");
        assertThat(entries.get("lib/shared/relation_selector.dart")).contains("DropdownButtonFormField", "FilterChip", "ApiClient().getList");
        assertThat(entries.get("lib/core/ollama_config.dart")).contains("AI_ENABLED", "OLLAMA_BASE_URL", "OLLAMA_MODEL");
        assertThat(entries.get("lib/core/app_config.dart")).contains("setApiBaseUrl", "http://10.0.2.2:8081/api");
        assertThat(entries.get("lib/shared/server_settings_button.dart")).contains("Servidor del backend", "192.168.1.10:8081/api");
        assertThat(entries.get("lib/core/app_theme.dart")).contains("ColorScheme.fromSeed", "scaffoldBackgroundColor", "CardThemeData");
        assertThat(entries.get("lib/shared/app_empty_state.dart")).contains("AppEmptyState", "primaryContainer");
        assertThat(entries.get("lib/main.dart")).contains("AppTheme.light", "debugShowCheckedModeBanner: false", "toolbarHeight: 76");
        assertThat(entries.get("lib/features/libros/pages/libro_page.dart")).contains("RefreshIndicator", "Card(child: ListTile", "Aún no hay registros");
        assertThat(entries.get("lib/core/offline_sync_service.dart")).contains("SharedPreferences", "Timer.periodic", "SyncPhase.conflict", "retryConflicts", "discardConflict");
        assertThat(entries.get("lib/shared/offline_status_banner.dart")).contains("Sin conexión", "Cambios por resolver", "No se sobrescribió ningún dato");
        assertThat(entries.get("lib/main.dart")).contains("OfflineSyncService.initialize", "OfflineStatusBanner");
        assertThat(entries.get("lib/ai/assistant_schema.dart")).contains("assistantAllowedPaths", "assistantResourceLabels", "assistantAutoGeneratedIdPaths", "assistantLocalResources", "LocalAssistantResource", "POST|PUT|DELETE", "/libros");
        assertThat(entries.get("lib/ai/ollama_assistant_service.dart")).contains("OllamaConfig.generateUri", "AssistantProposal", "offline = false", "_local(String instruction)", "assistantLocalResources", "assistantAllowedPaths", "assistantAutoGeneratedIdPaths", "IdGenerator.v4", "actionLabel", "ApiClient");
        assertThat(entries.get("lib/ai/assistant_sheet.dart")).contains("Revisar datos", "Aplicar cambios", "Usar micrófono", "Interpretado sin conexión", "_operationReview");
        assertThat(entries.get("lib/ai/voice_service.dart")).contains("dart.library.io");
        assertThat(entries.get("lib/ai/voice_service_web.dart")).contains("SpeechToText", "es-ES", "speech_to_text");
        assertThat(entries.get("lib/ai/voice_service_vosk.dart")).contains("VoskFlutterPlugin", "vosk-model-small-es-0.42.zip", "createModel(modelPath)", "onResult", "onPartial", "static dynamic _speechService");
        assertThat(entries.get("lib/shared/assistant_button.dart")).contains("Asistente local", "AssistantSheet");
        assertThat(entries.get("README.md")).contains("flutter create .", "10.0.2.2", "relaciones se seleccionan", "Ollama es opcional");
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
