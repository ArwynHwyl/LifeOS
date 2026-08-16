package com.example.demo.learningassistant.service;

import com.example.demo.shared.config.GeminiAiProperties;
import com.example.demo.shared.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiAssistantAiClient implements AssistantAiClient {
    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "propertyOrdering", List.of("teachingPoints", "examples", "followUpQuestions"),
            "properties", Map.of(
                    "teachingPoints", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 1,
                            "maxItems", 1,
                            "description", "One concise explanation or one next hint. Do not ask a question here."),
                    "examples", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 0,
                            "maxItems", 1,
                            "description", "At most one example for EXPLAIN mode; empty for HINT mode."),
                    "followUpQuestions", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string"),
                            "minItems", 1,
                            "maxItems", 1,
                            "description", "One short question with exactly one question mark at the end.")),
            "required", List.of("teachingPoints", "examples", "followUpQuestions"),
            "additionalProperties", false);

    private final GeminiAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAssistantAiClient(GeminiAiProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(60_000);
        this.restClient = builder.requestFactory(requestFactory)
                .baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void stream(String prompt, Consumer<String> deltaConsumer) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant is not configured");
        }
        Map<String, Object> body = requestBody(prompt);
        boolean[] reachedTokenLimit = {false};
        restClient.post()
                .uri(uri -> uri.path("/v1beta/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse").build(properties.normalizedModel()))
                .header("x-goog-api-key", properties.apiKey().trim())
                .body(body)
                .exchange((request, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant is temporarily unavailable");
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) continue;
                            JsonNode chunk = objectMapper.readTree(line.substring(5).trim());
                            if ("MAX_TOKENS".equals(chunk.at("/candidates/0/finishReason").asText())) {
                                reachedTokenLimit[0] = true;
                            }
                            JsonNode parts = chunk.at("/candidates/0/content/parts");
                            if (!parts.isArray()) continue;
                            for (JsonNode part : parts) {
                                String text = part.path("text").asText("");
                                if (!text.isEmpty()) deltaConsumer.accept(text);
                            }
                        }
                    } catch (ApiException exception) {
                        throw exception;
                    } catch (Exception exception) {
                        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant stream failed");
                    }
                    if (reachedTokenLimit[0]) {
                        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                                "AI response reached its output limit");
                    }
                    return null;
                });
    }

    Map<String, Object> requestBody(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 2_048,
                        "thinkingConfig", Map.of("thinkingBudget", 0),
                        "responseMimeType", "application/json",
                        "responseJsonSchema", RESPONSE_SCHEMA
                )
        );
    }
}
