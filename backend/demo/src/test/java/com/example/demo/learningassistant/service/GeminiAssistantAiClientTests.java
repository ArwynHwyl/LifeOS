package com.example.demo.learningassistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.shared.config.GeminiAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiAssistantAiClientTests {
    private final GeminiAssistantAiClient client = new GeminiAssistantAiClient(
            new GeminiAiProperties("test-key", "gemini-2.5-flash"),
            RestClient.builder(), new ObjectMapper());

    @Test
    @SuppressWarnings("unchecked")
    void requestUsesStructuredArraysWithExactCardinality() {
        Map<String, Object> body = client.requestBody("prompt");
        Map<String, Object> generationConfig = (Map<String, Object>) body.get("generationConfig");
        Map<String, Object> schema = (Map<String, Object>) generationConfig.get("responseJsonSchema");
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        assertThat(generationConfig.get("responseMimeType")).isEqualTo("application/json");
        assertExactlyOneItem((Map<String, Object>) properties.get("teachingPoints"));
        assertExactlyOneItem((Map<String, Object>) properties.get("followUpQuestions"));
        assertThat(((Map<String, Object>) properties.get("examples")).get("maxItems")).isEqualTo(1);
        assertThat(schema.get("required")).isEqualTo(List.of(
                "teachingPoints", "examples", "followUpQuestions"));
    }

    private void assertExactlyOneItem(Map<String, Object> property) {
        assertThat(property.get("minItems")).isEqualTo(1);
        assertThat(property.get("maxItems")).isEqualTo(1);
    }
}
