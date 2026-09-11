package com.examensw1.umlcollab.features.ai.service;

/** Cliente intercambiable del proveedor que transforma una imagen UML en PlantUML. */
public interface DiagramImageAiClient {
    String generatePlantUml(byte[] image, String contentType);
}
