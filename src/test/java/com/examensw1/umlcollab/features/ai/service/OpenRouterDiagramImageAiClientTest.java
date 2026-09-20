package com.examensw1.umlcollab.features.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenRouterDiagramImageAiClientTest {
    private final OpenRouterDiagramImageAiClient client = new OpenRouterDiagramImageAiClient(null, new ObjectMapper());

    @Test
    void wrapsClassOnlyResponseReturnedByVisionModel() {
        assertThat(client.normalizePlantUml("class Usuario {\n  - nombre : String\n}\nUsuario -- Rol"))
                .isEqualTo("@startuml\nclass Usuario {\n  - nombre : String\n}\nUsuario -- Rol\n@enduml");
    }

    @Test
    void keepsOnlyPlantUmlBlockWhenModelAddsTextAroundIt() {
        assertThat(client.normalizePlantUml("Aquí está:\n```plantuml\n@startuml\nclass Libro\n@enduml\n```\nfin"))
                .isEqualTo("@startuml\nclass Libro\n@enduml");
    }

    @Test
    void rejectsAnswerWithoutUmlClasses() {
        assertThatThrownBy(() -> client.normalizePlantUml("No se distingue el diagrama."))
                .hasMessageContaining("no identificó clases UML");
    }
}
