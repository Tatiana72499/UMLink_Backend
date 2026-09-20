package com.examensw1.umlcollab.features.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class OpenRouterDiagramImageAiClient implements DiagramImageAiClient {
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final int COMPLEX_DIAGRAM_MAX_COMPLETION_TOKENS = 4096;
    private static final String INSTRUCTIONS = """
            Convierte la imagen completa en un único diagrama PlantUML válido. La imagen puede ser un diagrama
            UML de clases o un diagrama entidad-relación de base de datos. Devuelve exclusivamente PlantUML,
            sin Markdown ni explicación; debe iniciar con @startuml y terminar con @enduml.

            Haz internamente tres revisiones antes de responder: (1) inventario de todas las cajas/tablas/clases,
            incluso en bordes; (2) atributos, PK/FK y operaciones legibles de cada una; (3) cada conector,
            bucle, cardinalidad, herencia o tabla intermedia. No termines después de las primeras relaciones.
            Emite cada entidad exactamente una vez. Para un ERD, representa cada tabla o entidad como class y
            conserva las etiquetas PK/FK legibles como comentarios de atributo; una FK no sustituye su línea de
            relación si el conector es visible.

            Usa solo estos conectores: --, -> o --> para asociación; o-- agregación; *-- composición;
            <|-- herencia/generalización; <|.. realización; ..> dependencia. Usa <|-- únicamente si ves el
            triángulo vacío de generalización apuntando al padre; no deduzcas herencia por el nombre. Una relación
            recursiva tiene la misma entidad en ambos extremos y siempre debe usar --, ->, -->, o-- o *--,
            nunca <|--, <|.. ni ..>. Una entidad/tablas intermedia se representa con sus dos asociaciones visibles;
            no la elimines ni la fusiones con otra relación.

            Conserva cardinalidades de texto 1, 1..1, 0..1, 1..* y 0..*. Para notación crow's-foot, traduce
            barra a 1, círculo a 0 y pata de cuervo a *; por ejemplo círculo+pata = 0..* y barra+pata = 1..*.
            Escribe cada relación en una sola línea con alias y cardinalidades entre comillas, por ejemplo:
            Usuario "1" -- "0..*" Pedido : realiza. Incluye todas las relaciones cuyos dos extremos sean
            distinguibles, aun si cruzan otras líneas. No inventes texto ilegible: omite únicamente ese detalle.
            Antes de responder, comprueba que el conteo de clases coincida con el inventario visual y que todo
            conector identificado tenga su línea PlantUML.
            """;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${OPENROUTER_API_KEY:}")
    private String apiKey;

    @Value("${OPENROUTER_MODEL:openrouter/free}")
    private String model;

    @Value("${OPENROUTER_FALLBACK_MODELS:openrouter/free,inclusionai/ling-3.0-flash-vl:free}")
    private String fallbackModels;

    @Override
    public String generatePlantUml(byte[] image, String contentType) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("El análisis por IA no está configurado. Define OPENROUTER_API_KEY en el archivo .env del backend.");
        }

        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);


        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(model);
        for (String fallback : fallbackModels.split(",")) if (StringUtils.hasText(fallback)) candidates.add(fallback.trim());
        IllegalArgumentException lastFailure = null;
        for (String candidate : candidates) {
            try {
                String response = restClientBuilder.build().post()
                        .uri(OPENROUTER_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request(candidate, dataUrl))
                        .retrieve()
                        .body(String.class);
                return responseContent(response);
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                    throw new IllegalArgumentException("OpenRouter rechazó la clave configurada. Revisa OPENROUTER_API_KEY.");
                }
                lastFailure = new IllegalArgumentException("El proveedor de visión no pudo atender esta imagen (HTTP " + exception.getStatusCode().value() + ").");
            } catch (RestClientException exception) {
                lastFailure = new IllegalArgumentException("No pudimos conectar con el servicio de IA. Revisa tu conexión e inténtalo nuevamente.");
            } catch (IllegalArgumentException exception) {
                lastFailure = exception;
            }
        }
        throw new IllegalArgumentException("Los proveedores gratuitos de visión no pudieron analizar la imagen en este momento. Intenta nuevamente en unos minutos. " + (lastFailure == null ? "" : lastFailure.getMessage()));
    }

    private Map<String, Object> request(String candidate, String dataUrl) {
        return Map.of(
                "model", candidate,
                "max_tokens", COMPLEX_DIAGRAM_MAX_COMPLETION_TOKENS,
                "temperature", 0,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", INSTRUCTIONS),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))));
    }
    private String responseContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) throw new IllegalArgumentException("La IA no devolvió una propuesta de diagrama válida. Inténtalo con una imagen más clara.");
            return normalizePlantUml(content);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("La IA devolvió una respuesta no compatible. Inténtalo nuevamente.");
        }
    }

    /** Algunos modelos devuelven solo las clases aunque se les pida el bloque PlantUML completo. */
    String normalizePlantUml(String content) {
        String cleaned = content.replaceFirst("(?s)^\\s*```(?:plantuml|puml)?\\s*", "")
                .replaceFirst("(?s)\\s*```\\s*$", "").trim();
        int start = cleaned.indexOf("@startuml");
        int end = cleaned.lastIndexOf("@enduml");
        if (start >= 0 && end >= start) return cleaned.substring(start, end + "@enduml".length()).trim();
        if (cleaned.matches("(?s).*\\b(class|interface)\\b.*")) {
            String body = start >= 0 ? cleaned.substring(start + "@startuml".length()).trim() : cleaned;
            if (body.endsWith("@enduml")) body = body.substring(0, body.length() - "@enduml".length()).trim();
            return "@startuml\n" + body + "\n@enduml";
        }
        throw new IllegalArgumentException("La IA no identificó clases UML en la imagen. Usa una captura más nítida donde se vean los nombres y relaciones.");
    }
}
