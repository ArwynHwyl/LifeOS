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
                    {
                      "title": "short learner-facing title",
                      "content": "bite-size lesson content grounded in the extracted source text",
                      "mascotPrompt": "one short friendly quote that helps the learner frame this subtopic",
                      "interactionType": "NONE | GRAPH_2D | FORMULA_EXPLORER | VISUAL_LAYER | QUIZ | LOGIC_FLOW",
                      "interactionPrompt": "optional implementation idea for the interaction",
                      "interactionConfig": {
                        "type": "QUIZ",
                        "title": "Quick check",
                        "question": "Which statement is correct?",
                        "options": [
                          { "id": "a", "label": "Correct answer", "correct": true },
                          { "id": "b", "label": "Distractor", "correct": false }
                        ],
                        "explanation": "Brief feedback."
                      }
                    }
                  ]
                }

                Requirements:
                - Create 3 to 6 subTopics unless the admin requirements ask for another count.
                - Add an interaction only when it improves the lesson; otherwise use interactionType NONE and omit interactionConfig.
                - Use only these generated interaction types: QUIZ, GRAPH_2D, FORMULA_EXPLORER, VISUAL_LAYER, LOGIC_FLOW, or NONE.
                - interactionConfig.type must exactly match interactionType.
                - Compact config examples:
                  QUIZ: {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"explanation":"Because."}
                  GRAPH_2D: {"type":"GRAPH_2D","title":"Graph","expression":"2 * x + 1","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}
                  FORMULA_EXPLORER: {"type":"FORMULA_EXPLORER","title":"Formula","formula":"a * b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2}
                  VISUAL_LAYER: {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]}}
                  LOGIC_FLOW (SIMPLIFY): {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify logic","start":"P -> Q","target":"!P | Q","allowedLaws":["IMPLICATION"],"steps":[{"law":"IMPLICATION","result":"!P | Q","note":"Rewrite the implication as a disjunction."}],"feedback":{"success":"Correct!","failure":"Try again!"}}
                  LOGIC_FLOW (CIRCUIT): {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Gate circuit","expression":"(A & B) | !C","variables":["A","B","C"],"goal":"MATCH_OUTPUT","feedback":{"success":"Correct!","failure":"Incorrect inputs."}}
                - Use the selected source text as the factual basis.
                - Keep each content field concise and readable for learners.
                - Keep mascotPrompt to one concise sentence, without markdown.
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
                      "interactionType": "NONE | GRAPH_2D | FORMULA_EXPLORER | VISUAL_LAYER | QUIZ | LOGIC_FLOW | OTHER",
                      "interactionPrompt": "optional implementation idea for the interaction",
                      "subTopics": [
                        {
                          "title": "short learner-facing subtopic title",
                          "content": "bite-size lesson content grounded in the extracted source text",
                          "mascotPrompt": "one short friendly quote that helps the learner frame this subtopic",
                          "interactionType": "NONE | GRAPH_2D | FORMULA_EXPLORER | VISUAL_LAYER | QUIZ | LOGIC_FLOW",
                          "interactionPrompt": "optional implementation idea for the interaction",
                          "interactionConfig": {
                            "type": "QUIZ",
                            "title": "Quick check",
                            "question": "Which statement is correct?",
                            "options": [
                              { "id": "a", "label": "Correct answer", "correct": true },
                              { "id": "b", "label": "Distractor", "correct": false }
                            ],
                            "explanation": "Brief feedback."
                          }
                        }
                      ]
                    }
                  ]
                }

                Requirements:
                - Create 4 to 8 modules unless the admin prompt asks for another count.
                - Create 3 to 6 subTopics per module unless the admin prompt asks for another count.
                - Add subtopic interactions only when they improve the lesson; otherwise use interactionType NONE and omit interactionConfig.
                - Use only these generated subtopic interaction types: QUIZ, GRAPH_2D, FORMULA_EXPLORER, VISUAL_LAYER, LOGIC_FLOW, or NONE. Do not invent interaction types.
                - interactionConfig.type must exactly match interactionType.
                - Compact subtopic config examples:
                  QUIZ: {"type":"QUIZ","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}],"explanation":"Because."}
                  GRAPH_2D: {"type":"GRAPH_2D","title":"Graph","expression":"2 * x + 1","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}
                  FORMULA_EXPLORER: {"type":"FORMULA_EXPLORER","title":"Formula","formula":"a * b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2}
                  VISUAL_LAYER: {"type":"VISUAL_LAYER","title":"Venn visual","canvas":{"width":900,"height":520},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":260,"y":130,"width":260,"height":260,"color":"#ffd333"},{"id":"zone_b","label":"B","shape":"circle","x":380,"y":130,"width":260,"height":260,"color":"#8fb3ff"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]}}
                  LOGIC_FLOW (SIMPLIFY): {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify logic","start":"P -> Q","target":"!P | Q","allowedLaws":["IMPLICATION"],"steps":[{"law":"IMPLICATION","result":"!P | Q","note":"Rewrite the implication as a disjunction."}],"feedback":{"success":"Correct!","failure":"Try again!"}}
                  LOGIC_FLOW (CIRCUIT): {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Gate circuit","expression":"(A & B) | !C","variables":["A","B","C"],"goal":"MATCH_OUTPUT","feedback":{"success":"Correct!","failure":"Incorrect inputs."}}
                - Use the selected source text as the factual basis.
                - Keep mascotPrompt to one concise sentence, without markdown.
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
                            blankToNull(subTopicNode.path("mascotPrompt").asText("")),
                            parseInteractionType(subTopicNode.path("interactionType").asText("NONE")),
                            blankToNull(subTopicNode.path("interactionPrompt").asText("")),
                            parseInteractionConfig(subTopicNode.get("interactionConfig"))
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

    private List<GeneratedSubTopicDraft> parseSubTopicsFromNode(JsonNode subTopicsNode) throws com.fasterxml.jackson.core.JsonProcessingException {
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
                        blankToNull(subTopicNode.path("mascotPrompt").asText("")),
                        parseInteractionType(subTopicNode.path("interactionType").asText("NONE")),
                        blankToNull(subTopicNode.path("interactionPrompt").asText("")),
                        parseInteractionConfig(subTopicNode.get("interactionConfig"))
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

    private String parseInteractionConfig(JsonNode node) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return blankToNull(node.asText());
        }
        if (!node.isObject()) {
            return null;
        }
        return objectMapper.writeValueAsString(node);
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
