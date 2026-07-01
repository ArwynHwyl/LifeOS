package com.example.demo.course.service.generation;

import com.example.demo.course.service.generation.model.GeneratedCourseModuleDraft;
import com.example.demo.course.service.generation.model.GeneratedCourseOutlineDraft;
import com.example.demo.course.service.generation.model.GeneratedModuleDraft;
import com.example.demo.course.service.generation.model.GeneratedSubTopicDraft;

import com.example.demo.shared.config.GeminiAiProperties;
import com.example.demo.course.entity.Course;
import com.example.demo.course.entity.CourseModule;
import com.example.demo.course.entity.InteractionType;
import com.example.demo.shared.exception.ValidationException;
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

    private static final String INTERACTIVE_SELECTION_GUIDE = """

                Content-first interaction selection procedure (perform these steps in order for every subtopic):
                1. Write the source-grounded subtopic content and identify its specific learning objective before considering an interaction.
                2. Start with interactionType NONE. Select another type only when the learner action directly improves that learning objective.
                3. Use only formulas, variables, sets, relationships, terminology, and data already introduced by the source or that subtopic. A minor Set or Logic connection is enough only when it directly reinforces the objective without importing a new concept.
                4. Select interactionType first, then select one supported mode from the matrix below, then build only that type-and-mode config.
                5. Treat every config example below as syntax reference only. Examples are never a reason to select that interaction type.

                Relevance rules:
                - LOGIC_FLOW requires actual Boolean or propositional expressions, truth tables, logic gates, implication, or logical equivalence in the subtopic. Do not use it for general reasoning, prose arguments, algorithm steps, programming control flow, or cause-and-effect descriptions.
                - VISUAL_LAYER VISUALIZATION requires a source-backed diagram, set, region, overlap, layer, or spatial relationship that learners benefit from inspecting.
                - VISUAL_LAYER PRACTICE requires a quantitative Set/Venn task with source-backed totals and intersections. Do not turn an unrelated topic into a Venn exercise.
                - GRAPH_2D requires a source-backed function or quantitative relationship that benefits from plotting.
                - FORMULA_EXPLORER requires a source-backed formula with meaningful adjustable variables.
                - QUIZ is a conceptual-check fallback when recall, classification, or choosing a correct statement is useful.
                - Use NONE when none of these actions materially improves the subtopic. There is no minimum interactive quota and no requirement to use every type.

                Supported mode matrix for AI-generated configs:
                - GRAPH_2D: VISUALIZATION only. Never generate GRAPH_2D PRACTICE yet.
                - FORMULA_EXPLORER: VISUALIZATION only. Never generate FORMULA_EXPLORER PRACTICE yet.
                - VISUAL_LAYER: VISUALIZATION for inspection; PRACTICE only for the quantitative Set/Venn task described above.
                - LOGIC_FLOW CIRCUIT: VISUALIZATION for exploring an expression; PRACTICE for solving a source-backed Boolean circuit task.
                - LOGIC_FLOW SIMPLIFY: PRACTICE only.
                - QUIZ: PRACTICE only.

                Pedagogical ordering and optional mode pairs:
                - Order modules and subtopics as prerequisites or introduction, conceptual explanation, visualization, guided practice, then assessment when those stages exist in the source.
                - When the same source-backed concept genuinely benefits from both supported modes, create two adjacent subtopics: VISUALIZATION first to explain or explore, then PRACTICE to apply the same concept.
                - The paired subtopics must have distinct titles, content, and learning actions; do not duplicate lesson text merely to include both modes.
                - Create a pair only for a type whose two modes are supported above, and only when it fits within the requested subtopic count. Do not add extra subtopics beyond the requested range.
                - Do not pair GRAPH_2D or FORMULA_EXPLORER until their PRACTICE modes are enabled. QUIZ and LOGIC_FLOW SIMPLIFY are practice-only and do not need a same-type visualization pair.
                """;

    private static final String INTERACTIVE_CONFIG_GUIDE = """

                Interactive config contract:
                - Generate only fields listed below. Never invent field names.
                - Use exactly the type and mode selected with the supported mode matrix. Never mix fields from different modes.
                - Keep every id unique within its collection and keep all referenced ids valid.

                Complete VISUAL_LAYER VISUALIZATION example using every supported visual building block:
                {
                  "type": "VISUAL_LAYER",
                  "mode": "VISUALIZATION",
                  "title": "learner-facing title",
                  "canvas": { "width": 900, "height": 520, "backgroundText": "optional canvas instruction" },
                  "zones": [{
                    "id": "zone_a", "label": "A", "shape": "circle",
                    "x": 250, "y": 130, "width": 260, "height": 260,
                    "labelX": 35, "labelY": 30,
                    "color": "#ffd333", "highlightColor": "#ff8f1f", "highlightOpacity": 0.82,
                    "feedback": "This is set A."
                  }, {
                    "id": "zone_b", "label": "B", "shape": "rectangle",
                    "x": 430, "y": 150, "width": 260, "height": 220,
                    "labelX": 65, "labelY": 30,
                    "color": "#8fb3ff", "highlightColor": "#4f8cff", "highlightOpacity": 0.82,
                    "feedback": "This is set B."
                  }],
                  "elements": [{
                    "id": "choice_a", "label": "Highlight A", "kind": "button",
                    "x": 40, "y": 40, "width": 150, "height": 48
                  }, {
                    "id": "hotspot_b", "label": "Inspect B", "kind": "hotspot",
                    "x": 720, "y": 140, "width": 100, "height": 80
                  }, {
                    "id": "flow_a", "label": "Flow to A", "kind": "line",
                    "x": 200, "y": 80, "width": 180, "height": 100,
                    "x1": 200, "y1": 80, "x2": 380, "y2": 180,
                    "qx": 290, "qy": 90, "flow": "forward", "arrow": "end",
                    "color": "#3b6cb5", "strokeWidth": 3
                  }],
                  "interactions": [{
                    "triggerId": "choice_a", "effect": "HIGHLIGHT_ZONE", "targetZoneId": "zone_a",
                    "feedback": "A is highlighted."
                  }, {
                    "triggerId": "hotspot_b", "effect": "SHOW_FEEDBACK", "feedback": "This is set B."
                  }],
                  "overlap": {
                    "enabled": true,
                    "sourceZoneIds": ["zone_a", "zone_b"],
                    "inputs": [{
                      "id": "A_ONLY", "label": "A total", "zoneIds": ["zone_a"], "value": 11, "kind": "total"
                    }, {
                      "id": "B_ONLY", "label": "B total", "zoneIds": ["zone_b"], "value": 9, "kind": "total"
                    }, {
                      "id": "A_AND_B", "label": "A intersect B", "zoneIds": ["zone_a", "zone_b"], "value": 3, "kind": "intersection"
                    }]
                  }
                }
                VISUAL_LAYER rules:
                - canvas width is 320..2400 and height is 240..1800; every zone and element must remain inside it.
                - zones may contain all optional label/highlight/feedback fields shown above.
                - elements may be button, hotspot, or line. A line must include x, y, width, height, x1, y1, x2, and y2; qx, qy, flow, arrow, color, and strokeWidth are optional.
                - interactions use HIGHLIGHT_ZONE with a valid targetZoneId, or SHOW_FEEDBACK.
                - overlap is optional for VISUALIZATION. If enabled, sourceZoneIds must reference zones and inputs must contain every non-empty combination: each single-zone total and every intersection.
                - Overlap ids are deterministic from sorted zone ids: zone_a => A_ONLY; zone_a + zone_b => A_AND_B. Do not add overlap.values; the server derives exact region values from inputs.
                - PRACTICE VISUAL_LAYER is a Venn-building task: use 2 or 3 circle source zones, mode PRACTICE, non-empty prompt and feedback, enabled overlap, and complete inputs. Keep elements and interactions as empty arrays because the learner builds the zones.
                - VISUALIZATION must omit prompt and feedback.

                Complete VISUAL_LAYER PRACTICE example:
                {"type":"VISUAL_LAYER","mode":"PRACTICE","title":"Build the Venn diagram","prompt":"Arrange the circles and enter each region value.","canvas":{"width":900,"height":520,"backgroundText":"Arrange the sets to match the data."},"zones":[{"id":"zone_a","label":"A","shape":"circle","x":250,"y":130,"width":260,"height":260,"labelX":35,"labelY":30,"color":"#ffd333","highlightColor":"#ff8f1f","highlightOpacity":0.82,"feedback":"Set A"},{"id":"zone_b","label":"B","shape":"circle","x":390,"y":130,"width":260,"height":260,"labelX":65,"labelY":30,"color":"#8fb3ff","highlightColor":"#4f8cff","highlightOpacity":0.82,"feedback":"Set B"}],"elements":[],"interactions":[],"overlap":{"enabled":true,"sourceZoneIds":["zone_a","zone_b"],"inputs":[{"id":"A_ONLY","label":"A total","zoneIds":["zone_a"],"value":11,"kind":"total"},{"id":"B_ONLY","label":"B total","zoneIds":["zone_b"],"value":9,"kind":"total"},{"id":"A_AND_B","label":"A intersect B","zoneIds":["zone_a","zone_b"],"value":3,"kind":"intersection"}]},"feedback":{"success":"Correct!","failure":"Check the overlap and region values."}}

                LOGIC_FLOW supports CIRCUIT and SIMPLIFY. Use only these logical operators: ! or ¬, & or ∧, | or ∨, ^ or ⊕, -> or →, <-> or ↔, parentheses, T, and F. Variables are single uppercase letters and there may be at most 8. Always use the same uppercase identifiers in expression and variables.
                CIRCUIT shape:
                {"type":"LOGIC_FLOW","kind":"CIRCUIT","mode":"PRACTICE","title":"Gate circuit","prompt":"Toggle the inputs to match the required output.","expression":"(A & B) | !C","variables":["A","B","C"],"goal":"MATCH_OUTPUT","feedback":{"success":"Correct!","failure":"Recheck the input values."}}
                - CIRCUIT goal is MATCH_OUTPUT, TRUE, FALSE, or EXPLORE. variables must contain only variables present in expression.
                - CIRCUIT may use VISUALIZATION or PRACTICE. PRACTICE requires success and failure feedback. VISUALIZATION omits feedback.
                - CIRCUIT must omit start, target, steps, and allowedLaws.

                SIMPLIFY shape:
                {"type":"LOGIC_FLOW","kind":"SIMPLIFY","mode":"PRACTICE","title":"Simplify the implication","prompt":"Apply the implication law.","start":"P -> Q","target":"!P | Q","steps":[{"law":"IMPLICATION","lawId":"IMPLICATION","from":"P -> Q","to":"!P | Q","result":"!P | Q","note":"Rewrite the implication as a disjunction."}],"allowedLaws":["IMPLICATION"],"feedback":{"success":"Correct!","failure":"Try the implication law first."}}
                - SIMPLIFY always uses PRACTICE and must include start, target, at least one valid step, allowedLaws, and feedback.
                - Every step must include law and result so InteractivePreview can render and grade the sequence. from, to, lawId, and note may also be included.
                - law, lawId, and allowedLaws may only use: DOUBLE_NEGATION, DE_MORGAN, DISTRIBUTIVE, IDENTITY, DOMINATION, IDEMPOTENT, COMPLEMENT, ABSORPTION, COMMUTATIVE, ASSOCIATIVE, IMPLICATION.
                - Each step result must be logically equivalent to the previous expression, and target must equal the final step result.
                - SIMPLIFY must omit expression, variables, and goal.
                """;

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
                        ]
                      }
                    }
                  ]
                }

                Requirements:
                - Create 3 to 6 subTopics unless the admin requirements ask for another count.
                - Add an interaction only when it improves the lesson; otherwise use interactionType NONE and omit interactionConfig.
                - Use only these generated interaction types: QUIZ, GRAPH_2D, FORMULA_EXPLORER, VISUAL_LAYER, LOGIC_FLOW, or NONE.
                - interactionConfig.type must exactly match interactionType.
                """ + INTERACTIVE_SELECTION_GUIDE + """
                - Compact config examples for non-visual and non-logic types:
                  QUIZ: {"type":"QUIZ","mode":"PRACTICE","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}
                  GRAPH_2D: {"type":"GRAPH_2D","mode":"VISUALIZATION","title":"Graph","expression":"2 * x + 1","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}
                  FORMULA_EXPLORER: {"type":"FORMULA_EXPLORER","mode":"VISUALIZATION","title":"Formula","formula":"a * b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2}
                """ + INTERACTIVE_CONFIG_GUIDE + """
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
                            ]
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
                """ + INTERACTIVE_SELECTION_GUIDE + """
                - Compact subtopic config examples for non-visual and non-logic types:
                  QUIZ: {"type":"QUIZ","mode":"PRACTICE","title":"Quiz","question":"Pick one","options":[{"id":"a","label":"A","correct":true},{"id":"b","label":"B","correct":false}]}
                  GRAPH_2D: {"type":"GRAPH_2D","mode":"VISUALIZATION","title":"Graph","expression":"2 * x + 1","xMin":-5,"xMax":5,"yMin":-10,"yMax":10,"sampleCount":100}
                  FORMULA_EXPLORER: {"type":"FORMULA_EXPLORER","mode":"VISUALIZATION","title":"Formula","formula":"a * b","variables":[{"name":"a","label":"A","min":1,"max":10,"step":1,"initial":2},{"name":"b","label":"B","min":1,"max":10,"step":1,"initial":3}],"precision":2}
                """ + INTERACTIVE_CONFIG_GUIDE + """
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
