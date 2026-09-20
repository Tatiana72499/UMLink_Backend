package com.examensw1.umlcollab.features.generation.service;

import com.examensw1.umlcollab.features.diagram.dto.DiagramDetailsResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlAttributeResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlClassResponse;
import com.examensw1.umlcollab.features.diagram.dto.UmlRelationResponse;
import com.examensw1.umlcollab.features.diagram.model.RelationType;
import com.examensw1.umlcollab.features.diagram.service.DiagramService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Generates a reviewable Flutter CRUD starter from UML classes and their own attributes. */
@Service
@Slf4j
@RequiredArgsConstructor
public class FlutterGenerationService {
    private static final Set<String> DART_KEYWORDS = Set.of("class", "enum", "extends", "false", "final", "for", "if", "import", "in", "is", "new", "null", "return", "static", "super", "this", "true", "var", "void", "while", "with");
    private final DiagramService diagramService;

    public GeneratedFlutter generate(UUID diagramId) {
        GenerationModel model = GenerationModel.from(diagramService.getDetails(diagramId));
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            add(zip, "README.md", readme(model));
            add(zip, ".gitignore", ".dart_tool/\nbuild/\n.env\n");
            add(zip, "analysis_options.yaml", "include: package:flutter_lints/flutter.yaml\n");
            add(zip, "pubspec.yaml", pubspec(model));
            add(zip, "lib/core/app_config.dart", "import 'package:flutter/foundation.dart';\nclass AppConfig { const AppConfig._(); static const _configuredUrl = String.fromEnvironment('API_BASE_URL', defaultValue: ''); static String _apiBaseUrl = _configuredUrl.isNotEmpty ? _configuredUrl : kIsWeb || defaultTargetPlatform != TargetPlatform.android ? 'http://127.0.0.1:8081/api' : 'http://10.0.2.2:8081/api'; static String get apiBaseUrl => _apiBaseUrl; static void setApiBaseUrl(String value) { final normalized = value.trim().replaceFirst(RegExp(r'/$'), ''); final uri = Uri.tryParse(normalized); if (uri == null || !uri.hasScheme || (uri.scheme != 'http' && uri.scheme != 'https')) throw const FormatException('Ingresa una URL http:// o https:// válida.'); _apiBaseUrl = normalized; } }\n");
            add(zip, "lib/core/app_theme.dart", appTheme());
            add(zip, "lib/core/id_generator.dart", idGenerator());
            add(zip, "lib/core/offline_sync_service.dart", offlineSyncService());
            add(zip, "lib/shared/server_settings_button.dart", serverSettingsButton());
            add(zip, "lib/shared/app_empty_state.dart", appEmptyState());
            add(zip, "lib/shared/offline_status_banner.dart", offlineStatusBanner());
            add(zip, "lib/core/ollama_config.dart", "class OllamaConfig { const OllamaConfig._(); static const enabled = bool.fromEnvironment('AI_ENABLED', defaultValue: false); static const baseUrl = String.fromEnvironment('OLLAMA_BASE_URL', defaultValue: ''); static const model = String.fromEnvironment('OLLAMA_MODEL', defaultValue: ''); static bool get isConfigured => enabled && baseUrl.isNotEmpty && model.isNotEmpty; static Uri get generateUri => Uri.parse('${baseUrl.replaceFirst(RegExp(r'/$'), '')}/api/generate'); }\n");
            add(zip, "lib/ai/assistant_schema.dart", assistantSchema(model));
            add(zip, "lib/ai/ollama_assistant_service.dart", ollamaAssistantService());
            add(zip, "lib/ai/assistant_sheet.dart", assistantSheet());
            add(zip, "lib/ai/voice_service.dart", "export 'voice_service_stub.dart' if (dart.library.html) 'voice_service_web.dart' if (dart.library.io) 'voice_service_vosk.dart';\n");
            add(zip, "lib/ai/voice_service_stub.dart", voiceServiceStub());
            add(zip, "lib/ai/voice_service_web.dart", voiceServiceWeb());
            add(zip, "lib/ai/voice_service_vosk.dart", voiceServiceVosk());
            add(zip, "lib/shared/assistant_button.dart", assistantButton());
            add(zip, "assets/models/README.md", voskModelReadme());
            add(zip, "lib/core/api_client.dart", apiClient());
            add(zip, "lib/shared/relation_selector.dart", relationSelector());
            add(zip, "lib/main.dart", main(model));
            for (ClassModel item : model.classes()) addClassFiles(zip, item);
            zip.finish();
            log.info("Flutter generado para diagrama {}: {} clases", diagramId, model.classes().size());
            return new GeneratedFlutter(model.artifactName() + "-flutter.zip", bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("No pudimos preparar el archivo Flutter generado.", exception);
        }
    }

    private void addClassFiles(ZipOutputStream zip, ClassModel item) throws IOException {
        String feature = "lib/features/" + item.resourceName();
        add(zip, feature + "/models/" + item.fileStem() + ".dart", dartModel(item));
        add(zip, feature + "/data-access/" + item.fileStem() + "_api.dart", api(item));
        add(zip, feature + "/pages/" + item.fileStem() + "_page.dart", page(item));
    }

    private void add(ZipOutputStream zip, String path, String content) throws IOException { zip.putNextEntry(new ZipEntry(path)); zip.write(content.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }

    private String readme(GenerationModel model) {
        return "# " + model.applicationName() + " mobile\n\nGenerado desde UMLink. Revisa el código antes de producción.\n\n## Ejecutar\n\n```powershell\nflutter create .\nflutter pub get\nflutter run --dart-define=API_BASE_URL=http://10.0.2.2:8081/api\n```\n\n`10.0.2.2` conecta un emulador Android con el backend local. Para un teléfono físico usa la IP LAN del equipo.\n\n## Alcance\n\n- Modelos, cliente REST, listados y formularios CRUD para atributos propios y relaciones persistentes.\n- Las relaciones se seleccionan desde los recursos relacionados; el backend valida sus IDs antes de guardar.\n- Las operaciones de crear, editar y eliminar se guardan localmente cuando el backend no está disponible; la app reintenta sincronizarlas cada 12 segundos.\n- Los conflictos `409` no se descartan ni sobrescriben datos: quedan visibles para reintentar o descartar manualmente.\n- Ollama es opcional y se configura con `AI_ENABLED`, `OLLAMA_BASE_URL` y `OLLAMA_MODEL`; las operaciones se muestran y requieren confirmación. UMLink no usa Ollama ni guarda claves.\n- Copia `vosk-model-small-es-0.42.zip` sin descomprimir a `assets/models/`. Vosk funciona en Android, Windows y Linux; la web usa texto. Android requiere el permiso `RECORD_AUDIO`.\n- En Windows activa Modo de desarrollador antes de `flutter pub get` para que Flutter cree los enlaces de plugins necesarios para Vosk.\n";
    }

    private String pubspec(GenerationModel model) { return "name: " + model.packageName() + "\ndescription: Aplicación Flutter generada desde UMLink.\npublish_to: none\nversion: 0.1.0+1\nenvironment:\n  sdk: ^3.11.0\ndependencies:\n  flutter:\n    sdk: flutter\n  http: ^0.13.6\n  shared_preferences: ^2.3.2\n  speech_to_text: ^7.4.0\n  vosk_flutter: ^0.3.48\n  web: ^1.1.1\ndependency_overrides:\n  # Vosk 0.3.x declara una versión Android antigua; esta evita APIs Flutter retiradas.\n  permission_handler: ^12.0.3\ndev_dependencies:\n  flutter_test:\n    sdk: flutter\n  flutter_lints: ^6.0.0\nflutter:\n  uses-material-design: true\n  assets:\n    - assets/models/\n"; }

    private String appTheme() {
        return """
                import 'package:flutter/material.dart';

                class AppTheme {
                  const AppTheme._();
                  static ThemeData light() {
                    final scheme = ColorScheme.fromSeed(seedColor: const Color(0xFF315EFB), brightness: Brightness.light);
                    return ThemeData(
                      useMaterial3: true,
                      colorScheme: scheme,
                      scaffoldBackgroundColor: const Color(0xFFF6F8FC),
                      appBarTheme: AppBarTheme(backgroundColor: scheme.surface, foregroundColor: scheme.onSurface, elevation: 0, surfaceTintColor: Colors.transparent),
                      cardTheme: CardThemeData(elevation: 0, color: Colors.white, margin: EdgeInsets.zero, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: scheme.outlineVariant.withValues(alpha: .65)))),
                      inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: scheme.surfaceContainerLowest, contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15), border: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none), enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: scheme.outlineVariant)), focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(14), borderSide: BorderSide(color: scheme.primary, width: 2))),
                      filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(minimumSize: const Size(0, 48), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)))),
                      snackBarTheme: SnackBarThemeData(behavior: SnackBarBehavior.floating, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
                    );
                  }
                }
                """;
    }

    private String appEmptyState() {
        return """
                import 'package:flutter/material.dart';

                class AppEmptyState extends StatelessWidget {
                  const AppEmptyState({super.key, required this.title, required this.message, required this.icon});
                  final String title;
                  final String message;
                  final IconData icon;
                  @override Widget build(BuildContext context) {
                    final colors = Theme.of(context).colorScheme;
                    return Center(child: Padding(padding: const EdgeInsets.all(32), child: Column(mainAxisSize: MainAxisSize.min, children: [Container(padding: const EdgeInsets.all(18), decoration: BoxDecoration(color: colors.primaryContainer, borderRadius: BorderRadius.circular(22)), child: Icon(icon, color: colors.onPrimaryContainer, size: 34)), const SizedBox(height: 18), Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)), const SizedBox(height: 8), Text(message, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: colors.onSurfaceVariant))]));
                  }
                }
                """;
    }

    private String offlineSyncService() {
        return """
                import 'dart:async';
                import 'dart:convert';
                import 'package:flutter/foundation.dart';
                import 'package:http/http.dart' as http;
                import 'package:shared_preferences/shared_preferences.dart';
                import 'app_config.dart';

                enum SyncPhase { online, offline, syncing, conflict }

                class PendingOperation {
                  const PendingOperation({required this.id, required this.method, required this.path, this.body, required this.createdAt, this.conflict = false, this.error});
                  final String id;
                  final String method;
                  final String path;
                  final Map<String, dynamic>? body;
                  final DateTime createdAt;
                  final bool conflict;
                  final String? error;
                  PendingOperation copyWith({bool? conflict, String? error}) => PendingOperation(id: id, method: method, path: path, body: body, createdAt: createdAt, conflict: conflict ?? this.conflict, error: error ?? this.error);
                  Map<String, dynamic> toJson() => {'id': id, 'method': method, 'path': path, 'body': body, 'createdAt': createdAt.toIso8601String(), 'conflict': conflict, 'error': error};
                  factory PendingOperation.fromJson(Map<String, dynamic> json) => PendingOperation(id: json['id'].toString(), method: json['method'].toString(), path: json['path'].toString(), body: json['body'] is Map ? Map<String, dynamic>.from(json['body'] as Map) : null, createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? '') ?? DateTime.now(), conflict: json['conflict'] == true, error: json['error']?.toString());
                }

                class SyncSnapshot {
                  const SyncSnapshot(this.phase, this.pending, this.conflicts);
                  final SyncPhase phase;
                  final int pending;
                  final int conflicts;
                }

                class OfflineSyncService {
                  OfflineSyncService._();
                  static const _storageKey = 'umlink.pending_operations.v1';
                  static final state = ValueNotifier(const SyncSnapshot(SyncPhase.online, 0, 0));
                  static final List<PendingOperation> _operations = [];
                  static SharedPreferences? _preferences;
                  static Timer? _timer;
                  static List<PendingOperation> get conflicts => List.unmodifiable(_operations.where((item) => item.conflict));

                  static Future<void> initialize() async {
                    _preferences = await SharedPreferences.getInstance();
                    final raw = _preferences?.getString(_storageKey);
                    if (raw != null) { final decoded = jsonDecode(raw); if (decoded is List) _operations.addAll(decoded.whereType<Map>().map((item) => PendingOperation.fromJson(Map<String, dynamic>.from(item)))); }
                    _publish();
                    _timer ??= Timer.periodic(const Duration(seconds: 12), (_) => sync());
                    if (_operations.any((item) => !item.conflict)) unawaited(sync());
                  }

                  static Future<void> enqueue(String method, String path, [Map<String, dynamic>? body]) async {
                    _operations.add(PendingOperation(id: '${DateTime.now().microsecondsSinceEpoch}-$method', method: method, path: path, body: body == null ? null : Map<String, dynamic>.from(body), createdAt: DateTime.now()));
                    await _persist();
                    _publish(SyncPhase.offline);
                  }

                  static Future<void> sync() async {
                    final pending = _operations.where((item) => !item.conflict).toList();
                    if (pending.isEmpty) { _publish(_operations.any((item) => item.conflict) ? SyncPhase.conflict : SyncPhase.online); return; }
                    _publish(SyncPhase.syncing);
                    for (final operation in pending) {
                      try {
                        final response = await _send(operation);
                        if (response.statusCode >= 200 && response.statusCode < 300) { _operations.removeWhere((item) => item.id == operation.id); await _persist(); continue; }
                        final message = _message(response.body);
                        _replace(operation.copyWith(conflict: true, error: message));
                        await _persist();
                      } on http.ClientException { _publish(SyncPhase.offline); return; } on TimeoutException { _publish(SyncPhase.offline); return; }
                    }
                    _publish(_operations.any((item) => item.conflict) ? SyncPhase.conflict : SyncPhase.online);
                  }

                  static Future<void> retryConflicts() async { for (var index = 0; index < _operations.length; index++) { if (_operations[index].conflict) _operations[index] = _operations[index].copyWith(conflict: false, error: ''); } await _persist(); await sync(); }
                  static Future<void> discardConflict(String id) async { _operations.removeWhere((item) => item.id == id && item.conflict); await _persist(); _publish(); }
                  static void markOnline() { if (_operations.any((item) => !item.conflict)) { unawaited(sync()); } else { _publish(_operations.any((item) => item.conflict) ? SyncPhase.conflict : SyncPhase.online); } }
                  static void markOffline() => _publish(SyncPhase.offline);
                  static Future<http.Response> _send(PendingOperation operation) { final uri = Uri.parse('${AppConfig.apiBaseUrl}${operation.path}'); final body = operation.body == null ? null : jsonEncode(operation.body); return switch (operation.method) { 'POST' => http.post(uri, headers: {'Content-Type': 'application/json'}, body: body).timeout(const Duration(seconds: 8)), 'PUT' => http.put(uri, headers: {'Content-Type': 'application/json'}, body: body).timeout(const Duration(seconds: 8)), 'DELETE' => http.delete(uri).timeout(const Duration(seconds: 8)), _ => Future.value(http.Response('', 400)) }; }
                  static String _message(String raw) { try { final decoded = jsonDecode(raw); if (decoded is Map && decoded['message'] != null) return decoded['message'].toString(); } catch (_) {} return raw.isEmpty ? 'No pudimos sincronizar el cambio.' : raw; }
                  static void _replace(PendingOperation value) { final index = _operations.indexWhere((item) => item.id == value.id); if (index >= 0) _operations[index] = value; }
                  static Future<void> _persist() => _preferences?.setString(_storageKey, jsonEncode(_operations.map((item) => item.toJson()).toList())) ?? Future.value();
                  static void _publish([SyncPhase? phase]) { final conflicts = _operations.where((item) => item.conflict).length; state.value = SyncSnapshot(phase ?? (conflicts > 0 ? SyncPhase.conflict : SyncPhase.online), _operations.length, conflicts); }
                }
                """;
    }

    private String offlineStatusBanner() {
        return """
                import 'package:flutter/material.dart';
                import '../core/offline_sync_service.dart';

                class OfflineStatusBanner extends StatelessWidget {
                  const OfflineStatusBanner({super.key});
                  @override Widget build(BuildContext context) => ValueListenableBuilder<SyncSnapshot>(valueListenable: OfflineSyncService.state, builder: (context, snapshot, _) {
                    if (snapshot.phase == SyncPhase.online && snapshot.pending == 0) return const SizedBox.shrink();
                    final colors = Theme.of(context).colorScheme;
                    final isConflict = snapshot.conflicts > 0;
                    final isSyncing = snapshot.phase == SyncPhase.syncing;
                    final background = isConflict ? colors.errorContainer : isSyncing ? colors.secondaryContainer : colors.tertiaryContainer;
                    final foreground = isConflict ? colors.onErrorContainer : isSyncing ? colors.onSecondaryContainer : colors.onTertiaryContainer;
                    final text = isConflict ? '${snapshot.conflicts} cambio(s) requieren revisión' : isSyncing ? 'Sincronizando ${snapshot.pending} cambio(s)…' : 'Sin conexión: ${snapshot.pending} cambio(s) guardados localmente';
                    return Material(color: background, child: InkWell(onTap: isConflict ? () => _showConflicts(context) : () => OfflineSyncService.sync(), child: Padding(padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10), child: Row(children: [Icon(isConflict ? Icons.warning_amber_rounded : isSyncing ? Icons.sync : Icons.cloud_off_outlined, color: foreground, size: 20), const SizedBox(width: 10), Expanded(child: Text(text, style: TextStyle(color: foreground, fontWeight: FontWeight.w600))), if (isConflict) Icon(Icons.chevron_right, color: foreground)]))));
                  });
                  Future<void> _showConflicts(BuildContext context) => showModalBottomSheet<void>(context: context, showDragHandle: true, builder: (sheetContext) => SafeArea(child: Padding(padding: const EdgeInsets.fromLTRB(20, 0, 20, 24), child: ValueListenableBuilder<SyncSnapshot>(valueListenable: OfflineSyncService.state, builder: (context, _, __) { final conflicts = OfflineSyncService.conflicts; return Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Cambios por resolver', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 8), const Text('No se sobrescribió ningún dato. Puedes reintentar cuando el conflicto esté resuelto en el servidor o descartar manualmente el cambio local.'), const SizedBox(height: 12), ...conflicts.map((item) => ListTile(contentPadding: EdgeInsets.zero, leading: const Icon(Icons.sync_problem_outlined), title: Text('${item.method} ${item.path}'), subtitle: Text(item.error ?? 'Conflicto pendiente'), trailing: IconButton(tooltip: 'Descartar cambio local', icon: const Icon(Icons.delete_outline), onPressed: () => OfflineSyncService.discardConflict(item.id))), const SizedBox(height: 8), FilledButton.icon(onPressed: OfflineSyncService.retryConflicts, icon: const Icon(Icons.refresh), label: const Text('Reintentar sincronización'))]); }))); 
                }
                """;
    }

    private String assistantSchema(GenerationModel model) {
        String resources = model.classes().stream().map(item -> "- `POST /" + item.resourceName() + "` crea " + item.javaName() + "; `PUT /" + item.resourceName() + "/{id}` actualiza; `DELETE /" + item.resourceName() + "/{id}` elimina.").reduce("", (left, right) -> left + "\\n" + right);
        String paths = model.classes().stream().map(item -> "'/" + item.resourceName() + "'").reduce((left, right) -> left + ", " + right).orElse("");
        String labels = model.classes().stream().map(item -> "'/" + item.resourceName() + "': '" + item.javaName() + "'").reduce((left, right) -> left + ", " + right).orElse("");
        String generatedIds = model.classes().stream().filter(ClassModel::autoGenerateTextId).map(item -> "'/" + item.resourceName() + "'").reduce((left, right) -> left + ", " + right).orElse("");
        String localResources = model.classes().stream().map(item -> {
            String fields = Stream.concat(Stream.of("'id'"), item.attributes().stream().map(attribute -> "'" + attribute.fieldName() + "'")).collect(Collectors.joining(", "));
            String primaryField = item.attributes().isEmpty() ? "id" : item.attributes().getFirst().fieldName();
            return "LocalAssistantResource(path: '/" + item.resourceName() + "', singular: '" + assistantSingular(item.resourceName()) + "', plural: '" + item.resourceName() + "', fields: [" + fields + "], primaryField: '" + primaryField + "')";
        }).collect(Collectors.joining(", "));
        return "const assistantAllowedPaths = <String>{" + paths + "};\nconst assistantResourceLabels = <String, String>{" + labels + "};\nconst assistantAutoGeneratedIdPaths = <String>{" + generatedIds + "};\nclass LocalAssistantResource { const LocalAssistantResource({required this.path, required this.singular, required this.plural, required this.fields, required this.primaryField}); final String path; final String singular; final String plural; final List<String> fields; final String primaryField; }\nconst assistantLocalResources = <LocalAssistantResource>[" + localResources + "];\nconst assistantContract = '''Eres un asistente local para una aplicación CRUD generada desde UML. Convierte la instrucción del usuario en JSON estricto, sin markdown. Usa exactamente este formato: {\\\"summary\\\":\\\"texto breve\\\",\\\"operations\\\":[{\\\"method\\\":\\\"POST|PUT|DELETE\\\",\\\"path\\\":\\\"/recurso o /recurso/id\\\",\\\"body\\\":{}}]}. Nunca inventes rutas. Para crear o editar incluye solo campos que el usuario haya indicado; para identificadores manuales pide o genera un UUID si falta. Nunca ejecutes nada: solo propone operaciones. Recursos permitidos:" + resources + "\\n''';\n";
    }

    private String assistantSingular(String resource) {
        String normalized = resource.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("es") && normalized.length() > 3) return normalized.substring(0, normalized.length() - 2);
        if (normalized.endsWith("s") && normalized.length() > 2) return normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String ollamaAssistantService() {
        return """
                import 'dart:convert';
                import 'package:http/http.dart' as http;
                import '../core/api_client.dart';
                import '../core/id_generator.dart';
                import '../core/ollama_config.dart';
                import '../core/offline_sync_service.dart';
                import 'assistant_schema.dart';

                class AssistantOperation {
                  const AssistantOperation({required this.method, required this.path, this.body});
                  final String method;
                  final String path;
                  final Map<String, dynamic>? body;
                  factory AssistantOperation.fromJson(Map<String, dynamic> json) => AssistantOperation(method: json['method']?.toString().toUpperCase() ?? '', path: json['path']?.toString() ?? '', body: json['body'] is Map ? Map<String, dynamic>.from(json['body'] as Map) : null);
                  String get resourcePath { final parts = path.split('/').where((item) => item.isNotEmpty).toList(); return parts.isEmpty ? '' : '/${parts.first}'; }
                  String get resourceLabel => assistantResourceLabels[resourcePath] ?? resourcePath.replaceFirst('/', '').replaceAll('_', ' ');
                  String get actionLabel => switch (method) { 'POST' => 'Crear $resourceLabel', 'PUT' => 'Actualizar $resourceLabel', 'DELETE' => 'Eliminar $resourceLabel', _ => 'Modificar $resourceLabel' };
                }

                class AssistantProposal {
                  const AssistantProposal({required this.summary, required this.operations, this.offline = false});
                  final String summary;
                  final List<AssistantOperation> operations;
                  final bool offline;
                }

                class OllamaAssistantService {
                  OllamaAssistantService({http.Client? client}) : _client = client ?? http.Client();
                  final http.Client _client;
                  Future<AssistantProposal> propose(String instruction) async {
                    try { return _local(instruction); } on ApiException catch (offlineError) { if (!OllamaConfig.isConfigured) rethrow; try {
                    final response = await _client.post(OllamaConfig.generateUri, headers: {'Content-Type': 'application/json'}, body: jsonEncode({'model': OllamaConfig.model, 'stream': false, 'format': 'json', 'prompt': '$assistantContract\\nInstrucción: $instruction'})).timeout(const Duration(seconds: 12));
                    if (response.statusCode < 200 || response.statusCode >= 300) throw ApiException('Ollama no pudo interpretar la instrucción (${response.statusCode}).');
                    final envelope = jsonDecode(response.body) as Map<String, dynamic>;
                    final decoded = jsonDecode(envelope['response']?.toString() ?? '{}') as Map<String, dynamic>;
                    final operations = (decoded['operations'] as List? ?? const []).whereType<Map>().map((item) => _normalizeOperation(AssistantOperation.fromJson(Map<String, dynamic>.from(item)))).toList();
                    if (operations.isEmpty) throw const ApiException('No entendí una operación ejecutable. Intenta indicar acción, entidad y datos.');
                    for (final operation in operations) { final segments = operation.path.split('/').where((part) => part.isNotEmpty).toList(); final root = segments.isEmpty ? '/' : '/${segments.first}'; if (!{'POST', 'PUT', 'DELETE'}.contains(operation.method) || !assistantAllowedPaths.contains(root)) throw const ApiException('La propuesta contiene una operación no permitida.'); }
                    return AssistantProposal(summary: decoded['summary']?.toString() ?? 'Propuesta preparada.', operations: operations);
                    } catch (_) { throw offlineError; } }
                  }
                  AssistantProposal _local(String instruction) {
                    final text = instruction.trim(); final normalized = _normalizeText(text); LocalAssistantResource? resource;
                    for (final candidate in assistantLocalResources) { if (_word(normalized, candidate.singular) || _word(normalized, candidate.plural)) { resource = candidate; break; } }
                    if (resource == null) throw const ApiException('Sin conexión puedo crear, editar, eliminar o consultar una entidad conocida. Indica la acción y entidad.');
                    if (_action(normalized, const ['consultar', 'consulta', 'listar', 'lista', 'mostrar', 'muestra', 'buscar', 'busca', 'ver'])) return AssistantProposal(summary: 'Consulta local: abre la sección ${resource.plural} para ver los registros disponibles.', operations: const [], offline: true);
                    final id = RegExp(r'\b(?:id|identificador|codigo|código)\s*(?::|=|es)?\s*([A-Za-z0-9_-]+)', caseSensitive: false).firstMatch(text)?.group(1);
                    if (_action(normalized, const ['eliminar', 'elimina', 'borrar', 'borra'])) { if (id == null) throw ApiException('Indica el identificador para eliminar ${resource.singular}.'); return AssistantProposal(summary: 'Preparé la eliminación sin conexión.', operations: [AssistantOperation(method: 'DELETE', path: '${resource.path}/$id')], offline: true); }
                    final body = _fields(text, resource);
                    if (_action(normalized, const ['editar', 'edita', 'actualizar', 'actualiza', 'modificar', 'modifica', 'cambiar', 'cambia'])) { if (id == null) throw ApiException('Indica el identificador para editar ${resource.singular}.'); if (body.isEmpty) throw const ApiException('Indica al menos un dato para actualizar.'); return AssistantProposal(summary: 'Preparé la actualización sin conexión.', operations: [AssistantOperation(method: 'PUT', path: '${resource.path}/$id', body: body)], offline: true); }
                    if (_action(normalized, const ['crear', 'crea', 'agregar', 'agrega', 'registrar', 'registra', 'nuevo', 'nueva'])) { if (body.isEmpty && resource.fields.length > 1) throw ApiException('Indica un dato para crear ${resource.singular}. Ejemplo: “crear ${resource.singular} llamado Dune”.'); return AssistantProposal(summary: 'Preparé la creación sin conexión. Se sincronizará al recuperar el backend.', operations: [_normalizeOperation(AssistantOperation(method: 'POST', path: resource.path, body: body))], offline: true); }
                    throw const ApiException('No identifiqué la acción. Puedes decir crear, editar, eliminar o consultar.');
                  }
                  Map<String, dynamic> _fields(String text, LocalAssistantResource resource) { final body = <String, dynamic>{}; for (final field in resource.fields.where((item) => item != 'id')) { final words = field.replaceAllMapped(RegExp(r'([a-z])([A-Z])'), (match) => '${match[1]} ${match[2]}'); final match = RegExp('(?:^|[ ,])(?:con +)?${RegExp.escape(words)} *(?::|=|es)? *(.+)\\$', caseSensitive: false).firstMatch(text); if (match != null) body[field] = _clean(match.group(1)!); } if (!body.containsKey(resource.primaryField)) { final named = RegExp(r'\b(?:llamado|llamada|nombre|titulo|título)\s*(?::|=|es)?\s*(.+)$', caseSensitive: false).firstMatch(text); if (named != null) body[resource.primaryField] = _clean(named.group(1)!); } return body; }
                  bool _action(String value, List<String> words) => words.any((word) => _word(value, word));
                  bool _word(String value, String word) => RegExp('(^|[^a-z0-9])${RegExp.escape(_normalizeText(word))}([^a-z0-9]|\\$)').hasMatch(value);
                  String _clean(String value) => value.trim().replaceAll('"', '').replaceAll("'", '');
                  String _normalizeText(String value) => value.toLowerCase().replaceAll('á', 'a').replaceAll('é', 'e').replaceAll('í', 'i').replaceAll('ó', 'o').replaceAll('ú', 'u').replaceAll('ñ', 'n');
                  AssistantOperation _normalizeOperation(AssistantOperation operation) {
                    if (operation.method != 'POST' || !assistantAutoGeneratedIdPaths.contains(operation.resourcePath)) return operation;
                    final body = Map<String, dynamic>.from(operation.body ?? const {});
                    final currentId = body['id']?.toString().trim() ?? '';
                    if (currentId.isEmpty) body['id'] = IdGenerator.v4();
                    return AssistantOperation(method: operation.method, path: operation.path, body: body);
                  }
                  Future<void> execute(AssistantProposal proposal) async {
                    final api = ApiClient();
                    for (final operation in proposal.operations) { if (operation.method == 'DELETE') { await api.delete(operation.path); } else { await api.send(operation.method, operation.path, operation.body ?? <String, dynamic>{}); } }
                    OfflineSyncService.markOnline();
                  }
                }
                """;
    }

    private String voiceServiceStub() {
        return """
                import 'package:flutter/foundation.dart';

                class VoskVoiceService {
                  Future<void> start(ValueChanged<String> onText, {ValueChanged<String>? onError, VoidCallback? onStopped}) async => throw UnsupportedError('La voz offline Vosk está disponible en Android, Windows yLinux; no en Flutter web.');
                  Future<void> stop() async {}
                  Future<void> dispose() async {}
                }
                """;
    }

    private String voiceServiceWeb() {
        return """
                import 'package:flutter/foundation.dart';
                import 'package:speech_to_text/speech_to_text.dart' as stt;

                class VoskVoiceService {
                  final _speech = stt.SpeechToText();
                  bool _initialized = false;
                  bool _available = false;
                  Future<void> start(ValueChanged<String> onText, {ValueChanged<String>? onError, VoidCallback? onStopped}) async {
                    if (!_initialized) { _available = await _speech.initialize(onError: (error) => onError?.call(_errorMessage(error.errorMsg)), onStatus: (status) { if (status == 'done' || status == 'notListening') onStopped?.call(); }); _initialized = true; }
                    if (!_available) throw UnsupportedError('Chrome no pudo iniciar el reconocimiento. Permite el micrófono para este sitio y usa Chrome actualizado.');
                    await _speech.listen(onResult: (result) { final text = result.recognizedWords.trim(); if (text.isNotEmpty) onText(text); }, listenOptions: stt.SpeechListenOptions(localeId: 'es-ES', partialResults: true, listenMode: stt.ListenMode.dictation, cancelOnError: true));
                  }
                  Future<void> stop() => _speech.stop();
                  Future<void> dispose() => stop();
                  String _errorMessage(String code) => switch (code) { 'error_permission' || 'not-allowed' || 'service-not-allowed' => 'Chrome no tiene permiso para el micrófono. En el candado junto a la dirección, permite Micrófono y recarga la página.', 'error_no_match' || 'no-speech' => 'No se detectó voz. Acerca el micrófono e inténtalo nuevamente.', 'error_audio' || 'audio-capture' => 'Chrome no encuentra un micrófono disponible. Revisa el dispositivo de entrada de Windows.', 'error_network' || 'network' => 'El reconocimiento de Chrome necesita Internet. Para voz sin conexión usa Windows o Android con Vosk.', _ => 'No se pudo reconocer la voz ($code). Revisa el permiso del micrófono e inténtalo otra vez.', };
                }
                """;
    }

    private String voiceServiceVosk() {
        return """
                import 'dart:async';
                import 'dart:convert';
                import 'package:flutter/foundation.dart';
                import 'package:vosk_flutter/vosk_flutter.dart';

                class VoskVoiceService {
                  StreamSubscription? _resultSubscription;
                  StreamSubscription? _partialSubscription;
                  // Vosk admite una SpeechService por proceso Android. La instancia
                  // compartida permite abrir/cerrar el asistente sin reinicializarla.
                  static dynamic _speechService;
                  Future<void> start(ValueChanged<String> onText, {ValueChanged<String>? onError, VoidCallback? onStopped}) async {
                    try {
                      await stop();
                      if (_speechService == null) {
                        final vosk = VoskFlutterPlugin.instance();
                        final modelPath = await ModelLoader().loadFromAssets('assets/models/vosk-model-small-es-0.42.zip');
                        final model = await vosk.createModel(modelPath);
                        final recognizer = await vosk.createRecognizer(model: model, sampleRate: 16000);
                        _speechService = await vosk.initSpeechService(recognizer);
                      }
                      void deliver(String raw) { final text = _text(raw); if (text.isNotEmpty) onText(text); }
                      _resultSubscription = _speechService.onResult().listen(deliver, onError: (error) => onError?.call('Vosk no pudo procesar el audio: $error'));
                      _partialSubscription = _speechService.onPartial().listen(deliver, onError: (error) => onError?.call('Vosk no pudo procesar el audio: $error'));
                      await _speechService.start(onRecognitionError: (error) => onError?.call('Vosk informó un error de audio: $error'));
                    } catch (error) { onError?.call('No se pudo iniciar el reconocimiento offline: $error'); rethrow; }
                  }
                  Future<void> stop() async { await _resultSubscription?.cancel(); await _partialSubscription?.cancel(); _resultSubscription = null; _partialSubscription = null; await _speechService?.stop(); }
                  Future<void> dispose() => stop();
                  String _text(dynamic result) { try { final decoded = jsonDecode(result.toString()); if (decoded is Map) return (decoded['text'] ?? decoded['partial'] ?? '').toString(); } catch (_) {} return result.toString(); }
                }
                """;
    }

    private String voskModelReadme() {
        return "# Modelo Vosk\n\nCopia aquí `vosk-model-small-es-0.42.zip` para voz offline en Android, Windows o Linux. No descomprimas el archivo.\n\nEn Android agrega `<uses-permission android:name=\"android.permission.RECORD_AUDIO\" />` a `android/app/src/main/AndroidManifest.xml` y las reglas ProGuard recomendadas por Vosk. Flutter web usa texto porque Vosk no tiene soporte web.\n";
    }

    private String assistantButton() {
        return """
                import 'package:flutter/material.dart';
                import '../ai/assistant_sheet.dart';

                class AssistantButton extends StatelessWidget {
                  const AssistantButton({super.key, required this.onApplied});
                  final VoidCallback onApplied;
                  @override Widget build(BuildContext context) => IconButton(tooltip: 'Asistente local', icon: const Icon(Icons.auto_awesome_outlined), onPressed: () => showModalBottomSheet<void>(context: context, isScrollControlled: true, showDragHandle: true, builder: (_) => AssistantSheet(onApplied: onApplied)));
                }
                """;
    }

    private String assistantSheet() {
        return """
                import 'dart:async';
                import 'package:flutter/material.dart';
                import '../core/api_client.dart';
                import '../core/ollama_config.dart';
                import 'ollama_assistant_service.dart';
                import 'voice_service.dart';

                class AssistantSheet extends StatefulWidget {
                  const AssistantSheet({super.key, required this.onApplied});
                  final VoidCallback onApplied;
                  @override State<AssistantSheet> createState() => _AssistantSheetState();
                }

                class _AssistantSheetState extends State<AssistantSheet> {
                  final _controller = TextEditingController();
                  final _assistant = OllamaAssistantService();
                  final _voice = VoskVoiceService();
                  AssistantProposal? _proposal;
                  bool _working = false;
                  bool _listening = false;
                  String? _voiceStatus;

                  @override void dispose() { _controller.dispose(); unawaited(_voice.dispose()); super.dispose(); }
                  Future<void> _propose() async { final text = _controller.text.trim(); if (text.isEmpty) return; setState(() { _working = true; _proposal = null; }); try { final proposal = await _assistant.propose(text); if (mounted) setState(() => _proposal = proposal); } on ApiException catch (error) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message))); } catch (_) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No pudimos conectar con el asistente local.'))); } finally { if (mounted) setState(() => _working = false); } }
                  Future<void> _toggleVoice() async { try { if (_listening) { await _voice.stop(); if (mounted) setState(() { _listening = false; _voiceStatus = 'Escucha finalizada.'; }); return; } setState(() => _voiceStatus = 'Preparando el reconocimiento offline…'); await _voice.start((text) { if (mounted) setState(() => _controller.text = text); }, onError: (message) { if (mounted) setState(() { _listening = false; _voiceStatus = message; }); }, onStopped: () { if (mounted) setState(() { _listening = false; _voiceStatus = 'Escucha finalizada.'; }); }); if (mounted) setState(() { _listening = true; _voiceStatus = 'Escuchando en español. El texto aparecerá al hacer una pausa o al pulsar Detener.'; }); } on UnsupportedError catch (error) { if (mounted) setState(() => _voiceStatus = error.message?.toString()); } catch (error) { if (mounted) setState(() => _voiceStatus = 'No pudimos iniciar el reconocimiento offline: $error'); } }
                  Future<void> _confirm() async { final proposal = _proposal; if (proposal == null || proposal.operations.isEmpty) return; final accepted = await showDialog<bool>(context: context, builder: (dialogContext) => AlertDialog(title: const Text('Revisar datos'), content: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('Estos son los datos que se aplicarán.'), const SizedBox(height: 12), ...proposal.operations.map(_operationReview)])), actions: [TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Volver a editar')), FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Aplicar cambios'))])); if (accepted != true) return; setState(() => _working = true); try { await _assistant.execute(proposal); if (!mounted) return; widget.onApplied(); Navigator.pop(context); ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Cambios aplicados o guardados para sincronización.'))); } on ApiException catch (error) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message))); } finally { if (mounted) setState(() => _working = false); } }
                  Widget _operationReview(AssistantOperation operation) => Card(margin: const EdgeInsets.only(bottom: 10), child: Padding(padding: const EdgeInsets.all(12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [Icon(operation.method == 'DELETE' ? Icons.delete_outline : Icons.edit_note_outlined), const SizedBox(width: 8), Expanded(child: Text(operation.actionLabel, style: const TextStyle(fontWeight: FontWeight.w800)))]), if (operation.body != null && operation.body!.isNotEmpty) ...[const SizedBox(height: 8), ...operation.body!.entries.map((entry) => Padding(padding: const EdgeInsets.only(bottom: 4), child: Text('${_fieldLabel(entry.key)}: ${_valueText(entry.value)}')))] else if (operation.method == 'DELETE') ...[const SizedBox(height: 8), const Text('Se eliminará este registro.')]])));
                  String _fieldLabel(String value) { final words = value.replaceAllMapped(RegExp(r'([a-záéíóú])([A-Z])'), (match) => '${match[1]} ${match[2]}').replaceAll('Ids', ' relacionados'); return words.isEmpty ? 'Dato' : '${words[0].toUpperCase()}${words.substring(1)}'; }
                  String _valueText(dynamic value) => value is List ? value.join(', ') : value?.toString() ?? 'Sin especificar';

                  @override Widget build(BuildContext context) => SafeArea(child: Padding(padding: EdgeInsets.fromLTRB(20, 0, 20, 20 + MediaQuery.viewInsetsOf(context).bottom), child: SingleChildScrollView(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [Container(padding: const EdgeInsets.all(10), decoration: BoxDecoration(color: Theme.of(context).colorScheme.primaryContainer, borderRadius: BorderRadius.circular(14)), child: const Icon(Icons.auto_awesome_outlined)), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Asistente local', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)), Text(OllamaConfig.isConfigured ? 'Modo integrado disponible; Ollama amplía las instrucciones cuando está conectado.' : 'Modo integrado listo sin Internet para crear, editar, eliminar y consultar.', style: Theme.of(context).textTheme.bodySmall)]))]), const SizedBox(height: 20), TextField(controller: _controller, minLines: 2, maxLines: 5, textInputAction: TextInputAction.newline, decoration: const InputDecoration(labelText: '¿Qué deseas hacer?', hintText: 'Ej.: Crea un registro con sus datos'), onSubmitted: (_) => _propose()), const SizedBox(height: 12), Row(children: [OutlinedButton.icon(onPressed: _working ? null : _toggleVoice, icon: Icon(_listening ? Icons.stop_circle_outlined : Icons.mic_none_outlined), label: Text(_listening ? 'Detener voz' : 'Usar micrófono')), const SizedBox(width: 10), Expanded(child: FilledButton.icon(onPressed: _working ? null : _propose, icon: _working ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.arrow_upward), label: const Text('Interpretar')))]), if (_voiceStatus != null) ...[const SizedBox(height: 12), DecoratedBox(decoration: BoxDecoration(color: Theme.of(context).colorScheme.surfaceContainerHighest, borderRadius: BorderRadius.circular(12)), child: Padding(padding: const EdgeInsets.all(12), child: Text(_voiceStatus!, style: Theme.of(context).textTheme.bodySmall)))], if (_proposal != null) ...[const SizedBox(height: 18), Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [if (_proposal!.offline) ...[const Chip(avatar: Icon(Icons.offline_bolt_outlined, size: 18), label: Text('Interpretado sin conexión')), const SizedBox(height: 8)], Text(_proposal!.summary, style: const TextStyle(fontWeight: FontWeight.w700)), if (_proposal!.operations.isNotEmpty) ...[const SizedBox(height: 12), ..._proposal!.operations.map(_operationReview), FilledButton.icon(onPressed: _working ? null : _confirm, icon: const Icon(Icons.visibility_outlined), label: const Text('Revisar datos'))]]))]])));
                }
                """;
    }

    private String apiClient() {
        return """
                import 'dart:async';
                  import 'dart:convert';
                  import 'package:http/http.dart' as http;
                  import 'app_config.dart';
                  import 'id_generator.dart';
                  import 'offline_sync_service.dart';
                class ApiException implements Exception { const ApiException(this.message); final String message; }
                class NetworkUnavailableException implements Exception { const NetworkUnavailableException(); }
                class ApiClient {
                  ApiClient({http.Client? httpClient}) : _httpClient = httpClient ?? http.Client();
                  final http.Client _httpClient;
                  Future<List<Map<String, dynamic>>> getList(String path) async { final value = await _request('GET', path); if (value is! List) throw const ApiException('La respuesta no contiene una lista válida.'); return value.map((item) => Map<String, dynamic>.from(item as Map)).toList(); }
                  Future<Map<String, dynamic>> send(String method, String path, Map<String, dynamic> body, {bool autoGenerateId = false}) async { final id = body['id']; if (method == 'POST' && body.containsKey('id') && (id == null || id.toString().trim().isEmpty)) { if (autoGenerateId) { body['id'] = IdGenerator.v4(); } else { throw const ApiException('Ingresa un identificador antes de guardar.'); } } try { final value = await _request(method, path, body: body); if (value is! Map) throw const ApiException('La respuesta no contiene un registro válido.'); return Map<String, dynamic>.from(value); } on NetworkUnavailableException { await OfflineSyncService.enqueue(method, path, body); return Map<String, dynamic>.from(body); } }
                  Future<void> delete(String path) async { try { await _request('DELETE', path); } on NetworkUnavailableException { await OfflineSyncService.enqueue('DELETE', path); } }
                  String _errorMessage(String raw) { if (raw.isEmpty) return 'No pudimos completar la solicitud.'; try { final decoded = jsonDecode(raw); if (decoded is Map && decoded['message'] != null) return decoded['message'].toString(); } catch (_) {} return raw; }
                  Future<dynamic> _request(String method, String path, {Map<String, dynamic>? body}) async { final uri = Uri.parse('${AppConfig.apiBaseUrl}$path'); final headers = <String, String>{'Accept': 'application/json', if (body != null) 'Content-Type': 'application/json'}; final encodedBody = body == null ? null : jsonEncode(body); try { final response = switch (method) { 'GET' => await _httpClient.get(uri, headers: headers).timeout(const Duration(seconds: 8)), 'POST' => await _httpClient.post(uri, headers: headers, body: encodedBody).timeout(const Duration(seconds: 8)), 'PUT' => await _httpClient.put(uri, headers: headers, body: encodedBody).timeout(const Duration(seconds: 8)), 'DELETE' => await _httpClient.delete(uri, headers: headers).timeout(const Duration(seconds: 8)), _ => throw const ApiException('Método HTTP no permitido.') }; final raw = response.body; if (response.statusCode < 200 || response.statusCode >= 300) throw ApiException(_errorMessage(raw)); OfflineSyncService.markOnline(); return raw.isEmpty ? null : jsonDecode(raw); } on http.ClientException { OfflineSyncService.markOffline(); throw const NetworkUnavailableException(); } on TimeoutException { OfflineSyncService.markOffline(); throw const NetworkUnavailableException(); } }
                }
                """;
    }

    private String idGenerator() {
        return """
                import 'dart:math';

                class IdGenerator {
                  const IdGenerator._();
                  static final _random = Random.secure();
                  static String v4() {
                    final bytes = List<int>.generate(16, (_) => _random.nextInt(256));
                    bytes[6] = (bytes[6] & 0x0f) | 0x40;
                    bytes[8] = (bytes[8] & 0x3f) | 0x80;
                    final hex = bytes.map((value) => value.toRadixString(16).padLeft(2, '0')).join();
                    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
                  }
                }
                """;
    }

    private String relationSelector() {
        return """
                import 'package:flutter/material.dart';
                import '../core/api_client.dart';

                class RelationSelector extends StatefulWidget {
                  const RelationSelector({super.key, required this.label, required this.path, required this.multiple, required this.selectedIds, required this.onChanged});
                  final String label;
                  final String path;
                  final bool multiple;
                  final List<String> selectedIds;
                  final ValueChanged<List<String>> onChanged;
                  @override State<RelationSelector> createState() => _RelationSelectorState();
                }

                class _RelationSelectorState extends State<RelationSelector> {
                  late Future<List<Map<String, dynamic>>> _options;
                  @override void initState() { super.initState(); _options = ApiClient().getList(widget.path); }
                  String _label(Map<String, dynamic> item) => item.entries.where((entry) => entry.key != 'id' && entry.value != null).map((entry) => entry.value.toString()).firstOrNull ?? item['id']?.toString() ?? 'Sin identificador';
                  @override Widget build(BuildContext context) => FutureBuilder<List<Map<String, dynamic>>>(future: _options, builder: (context, snapshot) {
                    if (snapshot.connectionState != ConnectionState.done) return const Padding(padding: EdgeInsets.all(12), child: LinearProgressIndicator());
                    if (snapshot.hasError) return Text('No pudimos cargar ${widget.label.toLowerCase()}.');
                    final options = snapshot.data ?? const <Map<String, dynamic>>[];
                    if (!widget.multiple) return DropdownButtonFormField<String>(initialValue: widget.selectedIds.isEmpty ? null : widget.selectedIds.first, decoration: InputDecoration(labelText: widget.label), items: options.map((item) { final id = item['id']?.toString(); return id == null ? null : DropdownMenuItem(value: id, child: Text(_label(item))); }).whereType<DropdownMenuItem<String>>().toList(), onChanged: (value) => widget.onChanged(value == null ? const [] : [value]));
                    return FormField<List<String>>(initialValue: widget.selectedIds, builder: (state) => InputDecorator(decoration: InputDecoration(labelText: widget.label, errorText: state.errorText), child: Wrap(spacing: 8, runSpacing: 4, children: options.map((item) { final id = item['id']?.toString(); if (id == null) return const SizedBox.shrink(); final selected = widget.selectedIds.contains(id); return FilterChip(label: Text(_label(item)), selected: selected, onSelected: (enabled) { final next = [...widget.selectedIds]; enabled ? next.add(id) : next.remove(id); widget.onChanged(next); }); }).toList())));
                  });
                }
                """;
    }

    private String serverSettingsButton() {
        return """
                import 'package:flutter/material.dart';
                import '../core/app_config.dart';

                class ServerSettingsButton extends StatelessWidget {
                  const ServerSettingsButton({super.key, required this.onSaved});
                  final VoidCallback onSaved;
                  @override Widget build(BuildContext context) => IconButton(tooltip: 'Servidor local', icon: const Icon(Icons.settings_ethernet), onPressed: () async {
                    final controller = TextEditingController(text: AppConfig.apiBaseUrl);
                    final saved = await showDialog<bool>(context: context, builder: (dialogContext) => AlertDialog(title: const Text('Servidor del backend'), content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [const Text('Tu APK se conecta al backend Spring Boot; este backend usa PostgreSQL.'), const SizedBox(height: 12), TextField(controller: controller, keyboardType: TextInputType.url, decoration: const InputDecoration(labelText: 'URL API', hintText: 'http://192.168.1.10:8081/api'))]), actions: [TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancelar')), FilledButton(onPressed: () { try { AppConfig.setApiBaseUrl(controller.text); Navigator.pop(dialogContext, true); } on FormatException catch (error) { ScaffoldMessenger.of(dialogContext).showSnackBar(SnackBar(content: Text(error.message))); } }, child: const Text('Usar servidor'))]));
                    if (saved == true) onSaved();
                  });
                }
                """;
    }

    private String main(GenerationModel model) {
        String imports = model.classes().stream().map(item -> "import 'features/" + item.resourceName() + "/pages/" + item.fileStem() + "_page.dart';").reduce("", (left, right) -> left + right + "\n");
        String pages = model.classes().stream().map(item -> "_Destination('" + item.javaName() + "', " + item.javaName() + "Page(), Icons.view_in_ar_outlined)").reduce((left, right) -> left + ", " + right).orElse("");
        return "import 'package:flutter/material.dart';\nimport 'core/app_config.dart';\nimport 'core/app_theme.dart';\nimport 'core/offline_sync_service.dart';\nimport 'shared/assistant_button.dart';\nimport 'shared/offline_status_banner.dart';\nimport 'shared/server_settings_button.dart';\n" + imports + "Future<void> main() async { WidgetsFlutterBinding.ensureInitialized(); await OfflineSyncService.initialize(); runApp(const GeneratedApp()); }\nclass GeneratedApp extends StatelessWidget { const GeneratedApp({super.key}); @override Widget build(BuildContext context) => MaterialApp(debugShowCheckedModeBanner: false, title: '" + model.applicationName() + "', theme: AppTheme.light(), home: const GeneratedHome()); }\nclass GeneratedHome extends StatefulWidget { const GeneratedHome({super.key}); @override State<GeneratedHome> createState() => _GeneratedHomeState(); }\nclass _GeneratedHomeState extends State<GeneratedHome> { static const items = <_Destination>[" + pages + "]; var selected = 0; @override Widget build(BuildContext context) { final item = items[selected]; return Scaffold(appBar: AppBar(toolbarHeight: 76, title: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [Text('" + model.applicationName() + "', style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w700)), Text(item.label, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800))]), actions: [AssistantButton(onApplied: () => setState(() {})), ServerSettingsButton(onSaved: () => setState(() {})), const SizedBox(width: 8)]), body: Column(children: [const OfflineStatusBanner(), Expanded(child: KeyedSubtree(key: ValueKey(AppConfig.apiBaseUrl), child: item.page))]), bottomNavigationBar: NavigationBar(selectedIndex: selected, onDestinationSelected: (index) => setState(() => selected = index), destinations: items.map((item) => NavigationDestination(icon: Icon(item.icon), selectedIcon: Icon(item.icon), label: item.label)).toList())); }}\nclass _Destination { const _Destination(this.label, this.page, this.icon); final String label; final Widget page; final IconData icon; }\n";
    }

    private String dartModel(ClassModel item) {
        String parameters = item.attributes().stream().map(attribute -> "this." + attribute.fieldName()).reduce("this.id", (left, right) -> left + ", " + right) + item.relations().stream().map(relation -> "this." + relation.requestName()).reduce("", (left, right) -> left + ", " + right);
        String fields = item.attributes().stream().map(attribute -> "final " + attribute.dartType() + "? " + attribute.fieldName() + ";").reduce("", (left, right) -> left + " " + right) + item.relations().stream().map(RelationField::dartField).reduce("", (left, right) -> left + " " + right);
        String fromJson = Stream.concat(item.attributes().stream().map(attribute -> attribute.fieldName() + ": " + attribute.fromJson()), item.relations().stream().map(RelationField::fromJson)).collect(Collectors.joining(", "));
        String request = Stream.concat(item.generatedId() ? Stream.empty() : Stream.of("'id': id"), Stream.concat(item.attributes().stream().map(attribute -> "'" + attribute.fieldName() + "': " + attribute.fieldName()), item.relations().stream().map(RelationField::requestEntry))).collect(Collectors.joining(", "));
        String display = item.attributes().isEmpty() ? "id ?? 'Sin identificador'" : item.attributes().getFirst().fieldName() + "?.toString() ?? (id ?? 'Sin identificador')";
        return "class " + item.javaName() + " { const " + item.javaName() + "({" + parameters + "}); final String? id; " + fields + " factory " + item.javaName() + ".fromJson(Map<String, dynamic> json) => " + item.javaName() + "(id: json['id']?.toString(), " + fromJson + "); Map<String, dynamic> toRequestJson() => {" + request + "}; String get displayLabel => " + display + "; }\n";
    }

    private String api(ClassModel item) {
        return "import '../../../core/api_client.dart'; import '../models/" + item.fileStem() + ".dart'; class " + item.javaName() + "Api { " + item.javaName() + "Api({ApiClient? client}) : _client = client ?? ApiClient(); final ApiClient _client; static const _path = '/" + item.resourceName() + "'; Future<List<" + item.javaName() + ">> findAll() async => (await _client.getList(_path)).map(" + item.javaName() + ".fromJson).toList(); Future<" + item.javaName() + "> create(" + item.javaName() + " value) async => " + item.javaName() + ".fromJson(await _client.send('POST', _path, value.toRequestJson(), autoGenerateId: " + item.autoGenerateTextId() + ")); Future<" + item.javaName() + "> update(" + item.javaName() + " value) async { final id = value.id; if (id == null || id.isEmpty) throw const ApiException('No se puede actualizar un registro sin identificador.'); return " + item.javaName() + ".fromJson(await _client.send('PUT', '$_path/$id', value.toRequestJson())); } Future<void> delete(String id) => _client.delete('$_path/$id'); }\n";
    }

    private String page(ClassModel item) {
        String idController = item.generatedId() ? "" : item.autoGenerateTextId() ? "final idController = TextEditingController(text: value == null ? IdGenerator.v4() : value?.id ?? '');" : "final idController = TextEditingController(text: value?.id ?? '');";
        String controllers = item.attributes().stream().map(attribute -> "final " + attribute.fieldName() + "Controller = TextEditingController(text: value?." + attribute.fieldName() + "?.toString() ?? '');").reduce(idController, (left, right) -> left + " " + right);
        String idInput = item.generatedId() ? "" : item.autoGenerateTextId() ? "TextField(controller: idController, decoration: const InputDecoration(labelText: 'Identificador', helperText: 'UUID generado automáticamente; puedes modificarlo.')), " : "TextField(controller: idController, decoration: const InputDecoration(labelText: 'Identificador *', helperText: 'Obligatorio')), ";
        String inputs = idInput + item.attributes().stream().map(attribute -> "TextField(controller: " + attribute.fieldName() + "Controller, decoration: const InputDecoration(labelText: '" + attribute.fieldName() + " (" + attribute.dartType() + ")')), ").reduce("", String::concat);
        String relationState = item.relations().stream().map(relation -> relation.stateDeclaration()).reduce("", (left, right) -> left + " " + right);
        String relationInputs = item.relations().stream().map(RelationField::selector).reduce("", String::concat);
        String values = Stream.concat(item.attributes().stream().map(attribute -> attribute.fieldName() + ": " + attribute.fromText()), item.relations().stream().map(RelationField::valueParameter)).collect(Collectors.joining(", "));
        String resultId = item.generatedId() ? "value?.id" : "idController.text.trim().isEmpty ? null : idController.text.trim()";
        String idGeneratorImport = item.autoGenerateTextId() ? "import '../../../core/id_generator.dart'; " : "";
        return "import 'package:flutter/material.dart'; import '../../../core/api_client.dart'; " + idGeneratorImport + "import '../../../shared/app_empty_state.dart'; import '../../../shared/relation_selector.dart'; import '../data-access/" + item.fileStem() + "_api.dart'; import '../models/" + item.fileStem() + ".dart'; class " + item.javaName() + "Page extends StatefulWidget { const " + item.javaName() + "Page({super.key}); @override State<" + item.javaName() + "Page> createState() => _" + item.javaName() + "PageState(); } class _" + item.javaName() + "PageState extends State<" + item.javaName() + "Page> { final api = " + item.javaName() + "Api(); late Future<List<" + item.javaName() + ">> items; @override void initState() { super.initState(); items = api.findAll(); } void reload() => setState(() => items = api.findAll()); Future<void> edit([" + item.javaName() + "? value]) async { final isCreating = value == null; " + controllers + relationState + " final result = await showDialog<" + item.javaName() + ">(context: context, builder: (dialogContext) => AlertDialog(title: Text(isCreating ? 'Crear " + item.javaName() + "' : 'Editar " + item.javaName() + "'), content: StatefulBuilder(builder: (context, setDialogState) => SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [" + inputs + relationInputs + "]))), actions: [TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancelar')), FilledButton(onPressed: () => Navigator.pop(dialogContext, " + item.javaName() + "(id: " + resultId + ", " + values + ")), child: const Text('Guardar'))])); if (result == null || !mounted) return; try { if (isCreating) { await api.create(result); } else { await api.update(result); } reload(); } on ApiException catch (error) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message))); } } Future<void> remove(" + item.javaName() + " value) async { final id = value.id; if (id == null) return; await api.delete(id); if (mounted) reload(); } @override Widget build(BuildContext context) => Scaffold(body: FutureBuilder<List<" + item.javaName() + ">>(future: items, builder: (context, snapshot) { if (snapshot.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator()); if (snapshot.hasError) return const AppEmptyState(title: 'No pudimos cargar " + item.javaName() + "', message: 'Revisa la conexión con el servidor e inténtalo nuevamente.', icon: Icons.cloud_off_outlined); final values = snapshot.data ?? const <" + item.javaName() + ">[]; if (values.isEmpty) return AppEmptyState(title: 'Aún no hay registros', message: 'Crea tu primer registro de " + item.javaName() + " para comenzar.', icon: Icons.auto_awesome_mosaic_outlined); return RefreshIndicator(onRefresh: () async { reload(); await items; }, child: ListView.separated(padding: const EdgeInsets.fromLTRB(16, 16, 16, 104), itemCount: values.length, separatorBuilder: (_, __) => const SizedBox(height: 12), itemBuilder: (context, index) { final value = values[index]; final colors = Theme.of(context).colorScheme; return Card(child: ListTile(contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9), leading: CircleAvatar(backgroundColor: colors.primaryContainer, foregroundColor: colors.onPrimaryContainer, child: const Icon(Icons.inventory_2_outlined)), title: Text(value.displayLabel, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700)), subtitle: const Padding(padding: EdgeInsets.only(top: 4), child: Text('Toca para ver o editar el registro')), onTap: () => edit(value), trailing: IconButton(tooltip: 'Eliminar', icon: Icon(Icons.delete_outline, color: colors.error), onPressed: () => remove(value))); })); }), floatingActionButton: FloatingActionButton.extended(onPressed: () => edit(), icon: const Icon(Icons.add), label: const Text('Nuevo " + item.javaName() + "'))); }\n";
    }

    public record GeneratedFlutter(String fileName, byte[] content) {}
    private record AttributeModel(String fieldName, String dartType) {
        String fromJson() { String value = "json['" + fieldName + "']"; return switch (dartType) { case "int" -> value + " is num ? " + value + ".toInt() : int.tryParse(" + value + "?.toString() ?? '')"; case "double" -> value + " is num ? " + value + ".toDouble() : double.tryParse(" + value + "?.toString() ?? '')"; case "bool" -> value + " is bool ? " + value + " : " + value + "?.toString().toLowerCase() == 'true'"; default -> value + "?.toString()"; }; }
        String fromText() { return switch (dartType) { case "int" -> "int.tryParse(" + fieldName + "Controller.text)"; case "double" -> "double.tryParse(" + fieldName + "Controller.text)"; case "bool" -> fieldName + "Controller.text.trim().toLowerCase() == 'true'"; default -> fieldName + "Controller.text.trim().isEmpty ? null : " + fieldName + "Controller.text.trim()"; }; }
    }
    private record RelationField(String targetName, String targetResource, boolean multiple) {
        String requestName() { return targetResource + (multiple ? "Ids" : "Id"); }
        String variableName() { return "selected" + Character.toUpperCase(requestName().charAt(0)) + requestName().substring(1); }
        String dartField() { return "final " + (multiple ? "List<String>? " : "String? ") + requestName() + ";"; }
        String fromJson() { return multiple ? requestName() + ": (json['" + requestName() + "'] as List? ?? const []).map((item) => item.toString()).toList()" : requestName() + ": json['" + requestName() + "']?.toString()"; }
        String requestEntry() { return "'" + requestName() + "': " + requestName(); }
        String stateDeclaration() { return multiple ? "var " + variableName() + " = List<String>.of(value?." + requestName() + " ?? const <String>[]);" : "final existing" + variableName() + " = value?." + requestName() + "; var " + variableName() + " = existing" + variableName() + " == null ? <String>[] : [existing" + variableName() + "];"; }
        String selector() { return "RelationSelector(label: '" + targetName + "', path: '/" + targetResource + "', multiple: " + multiple + ", selectedIds: " + variableName() + ", onChanged: (ids) => setDialogState(() => " + variableName() + " = ids)), "; }
        String valueParameter() { return requestName() + ": " + (multiple ? variableName() : variableName() + ".isEmpty ? null : " + variableName() + ".first"); }
    }
    private record ClassModel(UUID id, String javaName, String resourceName, String fileStem, boolean generatedId, boolean autoGenerateTextId, List<AttributeModel> attributes, List<RelationField> relations) {}
    private record GenerationModel(String applicationName, String artifactName, String packageName, List<ClassModel> classes) {
        static GenerationModel from(DiagramDetailsResponse details) {
            if (details.classes().isEmpty()) throw new IllegalArgumentException("El diagrama debe tener al menos una clase para generar una aplicación Flutter.");
            Set<String> names = new HashSet<>();
            Map<UUID, ClassModel> classById = new LinkedHashMap<>();
            for (UmlClassResponse item : details.classes().stream().sorted(Comparator.comparing(UmlClassResponse::name)).toList()) {
                String className = className(item.name());
                if (!names.add(className.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Dos clases producen el mismo nombre Dart: " + className + ". Renómbralas antes de generar.");
                UmlAttributeResponse primaryKey = item.attributes().stream().filter(UmlAttributeResponse::primaryKey).findFirst().orElse(null);
                boolean generatedId = primaryKey == null;
                boolean autoGenerateTextId = primaryKey != null && ("String".equals(primaryKey.dataType()) || "UUID".equals(primaryKey.dataType()));
                classById.put(item.id(), new ClassModel(item.id(), className, resourceName(className), snake(className), generatedId, autoGenerateTextId, item.attributes().stream().filter(attribute -> !attribute.primaryKey()).map(GenerationModel::attribute).toList(), List.of()));
            }
            Map<UUID, List<RelationField>> relationsBySource = new LinkedHashMap<>();
            for (UmlRelationResponse relation : details.relations()) {
                if (relation.type() == RelationType.DEPENDENCY || relation.type() == RelationType.GENERALIZATION || relation.type() == RelationType.REALIZATION) continue;
                ClassModel source = classById.get(relation.sourceClassId());
                ClassModel target = classById.get(relation.targetClassId());
                if (source == null || target == null || source == target) continue;
                boolean targetMany = ("1..*".equals(relation.targetCardinality()) || "0..*".equals(relation.targetCardinality()));
                RelationField field = new RelationField(target.javaName(), target.resourceName(), targetMany);
                List<RelationField> fields = relationsBySource.computeIfAbsent(source.id(), ignored -> new java.util.ArrayList<>());
                if (fields.stream().noneMatch(existing -> existing.requestName().equals(field.requestName()))) fields.add(field);
            }
            List<ClassModel> classes = classById.values().stream().map(item -> new ClassModel(item.id(), item.javaName(), item.resourceName(), item.fileStem(), item.generatedId(), item.autoGenerateTextId(), item.attributes(), List.copyOf(relationsBySource.getOrDefault(item.id(), List.of())))).toList();
            String artifact = slug(details.diagram().name()); return new GenerationModel(details.diagram().name(), artifact, artifact.replace('-', '_'), classes);
        }
        private static AttributeModel attribute(UmlAttributeResponse item) { return new AttributeModel(fieldName(item.name()), dartType(item.dataType())); }
        private static String dartType(String type) { return switch (type) { case "Integer", "Long" -> "int"; case "Double" -> "double"; case "Boolean" -> "bool"; default -> "String"; }; }
        private static String className(String value) { String result = camel(value); if (result.isBlank()) throw new IllegalArgumentException("Una clase no tiene un nombre compatible con Dart."); return DART_KEYWORDS.contains(result.toLowerCase(Locale.ROOT)) ? result + "Model" : result; }
        private static String fieldName(String value) { String result = camel(value); if (result.isBlank()) throw new IllegalArgumentException("Un atributo no tiene un nombre compatible con Dart."); result = Character.toLowerCase(result.charAt(0)) + result.substring(1); return DART_KEYWORDS.contains(result) ? result + "Value" : result; }
        private static String resourceName(String value) { String result = Character.toLowerCase(value.charAt(0)) + value.substring(1); return result.endsWith("s") ? result.toLowerCase(Locale.ROOT) : result.toLowerCase(Locale.ROOT) + "s"; }
        private static String slug(String value) { String result = ascii(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", ""); return result.isBlank() ? "generated_model" : result; }
        private static String snake(String value) { return ascii(value).replaceAll("([a-z])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9]+", "_").replaceAll("(^_|_$)", "").toLowerCase(Locale.ROOT); }
        private static String camel(String value) { String cleaned = ascii(value).replaceAll("[^A-Za-z0-9]+", " ").trim(); StringBuilder result = new StringBuilder(); for (String word : cleaned.split("\\s+")) if (!word.isBlank()) result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1)); return result.toString(); }
        private static String ascii(String value) { return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }
    }
}
