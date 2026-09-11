package com.examensw1.umlcollab.features.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
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
    private static final String INSTRUCTIONS = """
            Analiza exclusivamente el diagrama de clases UML de la imagen. Devuelve solo PlantUML válido,
            sin Markdown ni explicación. Debe iniciar con @startuml y terminar con @enduml. Incluye clases,
            atributos, operaciones y relaciones que se distingan con suficiente certeza. Usa únicamente los
            conectores: --, o--, *--, <|--, <|.. y ..>. Para cardinalidades usa solo 1..1, 0..1 o 1..*.
            No inventes elementos ilegibles. Si no puedes identificar ninguna clase, devuelve un diagrama vacío.
            """;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${OPENROUTER_API_KEY:}")
    private String apiKey;

    @Value("${OPENROUTER_MODEL:openrouter/free}")
    private String model;

    @Override
    public String generatePlantUml(byte[] image, String contentType) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("El análisis por IA no está configurado. Define OPENROUTER_API_KEY en el archivo .env del backend.");
        }

        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);
        Map<String, Object> request = Map.of(
                "model", model,
                "max_tokens", 3000,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", INSTRUCTIONS),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))));

        try {
            String response = restClientBuilder.build().post()
                    .uri(OPENROUTER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            return responseContent(response);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new IllegalArgumentException("El servicio de IA alcanzó su límite gratuito. Espera unos minutos e inténtalo nuevamente.");
            }
            throw new IllegalArgumentException("No pudimos analizar la imagen con IA. Verifica tu clave de OpenRouter e inténtalo nuevamente.");
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("No pudimos conectar con el servicio de IA. Revisa tu conexión e inténtalo nuevamente.");
        }
    }

    private String responseContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) throw new IllegalArgumentException("La IA no devolvió una propuesta de diagrama válida. Inténtalo con una imagen más clara.");
            return content.replaceFirst("(?s)^\\s*```(?:plantuml|puml)?\\s*", "")
                    .replaceFirst("(?s)\\s*```\\s*$", "").trim();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("La IA devolvió una respuesta no compatible. Inténtalo nuevamente.");
        }
    }
}
