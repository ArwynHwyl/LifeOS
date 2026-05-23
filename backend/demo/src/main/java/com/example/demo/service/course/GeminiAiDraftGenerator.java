package com.example.demo.service.course;

import com.example.demo.config.GeminiAiProperties;
import com.example.demo.entity.course.Course;
import com.example.demo.entity.course.CourseModule;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.service.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Primary
@ConditionalOnExpression("'${app.ai.gemini.api-key:}' != ''")
class GeminiAiDraftGenerator implements AiDraftGenerator {

    private final GeminiAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    GeminiAiDraftGenerator(GeminiAiProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratedModuleDraft generateDraft(CourseModule module, String prompt) {
        String instruction = prompt + """

                Return only JSON with this exact shape:
                {
                  "subTopics": [
                    { "title": "short learner-facing title", "content": "bite-size lesson content grounded in the extracted source text" }
                  ]
                }

                Requirements:
                - Create 3 to 6 subTopics unless the admin requirements ask for another count.
                - Use the selected source text as the factual basis.
                - Keep each content field concise and readable for learners.
                - Do not include markdown fences or any text outside JSON.
                """;

        JsonNode response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.normalizedModel())
                .header("x-goog-api-key", properties.apiKey().trim())
                .body(requestBody(instruction))
                .retrieve()
                .body(JsonNode.class);

        String rawText = extractText(response);
        return new GeneratedModuleDraft(rawText, parseSubTopics(rawText));
    }

    @Override
    public GeneratedCourseOutlineDraft generateCourseOutline(Course course, String prompt) {
        String instruction = prompt + """

                Return only JSON with this exact shape:
                {
                  "modules": [
                    {
                      "title": "short learner-facing module title",
                      "description": "brief module description",
                      "interactionType": "NONE | THREE_JS | GRAPH_2D | FORMULA_EXPLORER | QUIZ | OTHER",
                      "interactionPrompt": "optional implementation idea for the interaction",
                      "subTopics": [
                        {
                          "title": "short learner-facing subtopic title",
                          "content": "bite-size lesson content grounded in the extracted source text",
                          "interactionType": "NONE | THREE_JS | GRAPH_2D | FORMULA_EXPLORER | QUIZ | OTHER",
                          "interactionPrompt": "optional implementation idea for the interaction"
                        }
                      ]
                    }
                  ]
                }

                Requirements:
                - Create 4 to 8 modules unless the admin prompt asks for another count.
                - Create 3 to 6 subTopics per module unless the admin prompt asks for another count.
                - Prefer interactive ideas that make mathematics visible for software engineering learners.
                - Use the selected source text as the factual basis.
                - Do not include markdown fences or any text outside JSON.
                """;

        JsonNode response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.normalizedModel())
                .header("x-goog-api-key", properties.apiKey().trim())
                .body(requestBody(instruction))
                .retrieve()
                .body(JsonNode.class);

        String rawText = extractText(response);
        return new GeneratedCourseOutlineDraft(rawText, parseCourseModules(rawText));
    }

    private Map<String, Object> requestBody(String instruction) {
        return Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", instruction))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.35,
                        "responseMimeType", "application/json"
                )
        );
    }

    private String extractText(JsonNode response) {
        JsonNode parts = response == null ? null : response.at("/candidates/0/content/parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            throw new ValidationException("Gemini returned no draft content");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            String value = part.path("text").asText("");
            if (!value.isBlank()) {
                text.append(value);
            }
        }
        String rawText = text.toString().trim();
        if (rawText.isBlank()) {
            throw new ValidationException("Gemini returned empty draft content");
        }
        return rawText;
    }

    private List<GeneratedSubTopicDraft> parseSubTopics(String rawText) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFence(rawText));
            JsonNode subTopicsNode = root.path("subTopics");
            if (!subTopicsNode.isArray() || subTopicsNode.isEmpty()) {
                throw new ValidationException("Gemini returned no sub-topics");
            }
            List<GeneratedSubTopicDraft> subTopics = new ArrayList<>();
            for (JsonNode subTopicNode : subTopicsNode) {
                String title = subTopicNode.path("title").asText("").trim();
                String content = subTopicNode.path("content").asText("").trim();
                if (!title.isBlank() && !content.isBlank()) {
                    subTopics.add(new GeneratedSubTopicDraft(
                            title,
                            content,
                            parseInteractionType(subTopicNode.path("interactionType").asText("NONE")),
                            blankToNull(subTopicNode.path("interactionPrompt").asText(""))
                    ));
                }
            }
            if (subTopics.isEmpty()) {
                throw new ValidationException("Gemini returned no usable sub-topics");
            }
            return subTopics;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ValidationException("Gemini returned invalid JSON draft content");
        }
    }

    private List<GeneratedCourseModuleDraft> parseCourseModules(String rawText) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFence(rawText));
            JsonNode modulesNode = root.path("modules");
            if (!modulesNode.isArray() || modulesNode.isEmpty()) {
                throw new ValidationException("Gemini returned no course modules");
            }
            List<GeneratedCourseModuleDraft> modules = new ArrayList<>();
            for (JsonNode moduleNode : modulesNode) {
                String title = moduleNode.path("title").asText("").trim();
                String description = moduleNode.path("description").asText("").trim();
                List<GeneratedSubTopicDraft> subTopics = parseSubTopicsFromNode(moduleNode.path("subTopics"));
                if (!title.isBlank() && !subTopics.isEmpty()) {
                    modules.add(new GeneratedCourseModuleDraft(
                            title,
                            blankToNull(description),
                            parseInteractionType(moduleNode.path("interactionType").asText("NONE")),
                            blankToNull(moduleNode.path("interactionPrompt").asText("")),
                            subTopics
                    ));
                }
            }
            if (modules.isEmpty()) {
                throw new ValidationException("Gemini returned no usable course modules");
            }
            return modules;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ValidationException("Gemini returned invalid JSON course outline");
        }
    }

    private List<GeneratedSubTopicDraft> parseSubTopicsFromNode(JsonNode subTopicsNode) {
        if (!subTopicsNode.isArray() || subTopicsNode.isEmpty()) {
            return List.of();
        }
        List<GeneratedSubTopicDraft> subTopics = new ArrayList<>();
        for (JsonNode subTopicNode : subTopicsNode) {
            String title = subTopicNode.path("title").asText("").trim();
            String content = subTopicNode.path("content").asText("").trim();
            if (!title.isBlank() && !content.isBlank()) {
                subTopics.add(new GeneratedSubTopicDraft(
                        title,
                        content,
                        parseInteractionType(subTopicNode.path("interactionType").asText("NONE")),
                        blankToNull(subTopicNode.path("interactionPrompt").asText(""))
                ));
            }
        }
        return subTopics;
    }

    private InteractionType parseInteractionType(String value) {
        if (value == null || value.isBlank()) {
            return InteractionType.NONE;
        }
        try {
            return InteractionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return InteractionType.OTHER;
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String stripMarkdownFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewLine = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewLine == -1 || lastFence <= firstNewLine) {
            return trimmed;
        }
        return trimmed.substring(firstNewLine + 1, lastFence).trim();
    }
}
