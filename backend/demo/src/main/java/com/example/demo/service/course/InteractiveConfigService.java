package com.example.demo.service.course;

import com.example.demo.dto.course.InteractiveFieldDto;
import com.example.demo.dto.course.InteractiveTemplateDto;
import com.example.demo.entity.course.InteractionType;
import com.example.demo.service.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InteractiveConfigService {

    private static final int MAX_CONFIG_LENGTH = 50_000;
    private static final Set<String> COMMON_FIELDS = Set.of("type", "mode", "title");
    private static final Set<String> PRACTICE_FIELDS = Set.of("prompt", "successCondition", "feedback");
    private static final Set<String> GRAPH_FIELDS = Set.of(
            "type", "mode", "title", "expression", "xMin", "xMax", "yMin", "yMax", "sampleCount", "controls", "prompt", "successCondition", "feedback"
    );
    private static final Set<String> FORMULA_FIELDS = Set.of(
            "type", "mode", "title", "formula", "variables", "precision", "prompt", "successCondition", "feedback", "formulaOptions"
    );
    private static final Set<String> VARIABLE_FIELDS = Set.of(
            "name", "label", "min", "max", "step", "initial"
    );
    private static final Set<String> FORMULA_OPTION_FIELDS = Set.of(
            "id", "label", "formula", "description", "steps"
    );
    private static final Set<String> FORMULA_STEP_FIELDS = Set.of(
            "label", "expression", "explanation"
    );
    private static final Set<String> VISUAL_LAYER_FIELDS = Set.of("type", "mode", "title", "canvas", "zones", "elements", "interactions", "overlap", "prompt", "feedback");
    private static final Set<String> VISUAL_CANVAS_FIELDS = Set.of("width", "height", "backgroundText");
    private static final Set<String> VISUAL_ZONE_FIELDS = Set.of("id", "label", "shape", "x", "y", "width", "height", "labelX", "labelY", "color", "highlightColor", "highlightOpacity", "feedback");
    private static final Set<String> VISUAL_ELEMENT_FIELDS = Set.of(
            "id", "label", "kind", "x", "y", "width", "height",
            "x1", "y1", "x2", "y2", "qx", "qy", "flow", "arrow", "color", "strokeWidth"
    );
    private static final Set<String> VISUAL_INTERACTION_FIELDS = Set.of("triggerId", "effect", "targetZoneId", "feedback");
    private static final Set<String> VISUAL_OVERLAP_FIELDS = Set.of("enabled", "sourceZoneIds", "inputs", "values");
    private static final Set<String> VISUAL_OVERLAP_INPUT_FIELDS = Set.of("id", "label", "zoneIds", "value", "kind");
    private static final Set<String> VISUAL_OVERLAP_VALUE_FIELDS = Set.of("id", "label", "zoneIds", "value", "feedback");
    private static final Set<String> QUIZ_FIELDS = Set.of(
            "type", "mode", "title", "question", "options", "prompt", "successCondition", "feedback"
    );
    private static final Set<String> QUIZ_OPTION_FIELDS = Set.of("id", "label", "correct");
    private static final Set<String> LOGIC_FIELDS = Set.of(
            "type", "kind", "mode", "title", "prompt", "start", "target", "steps", "allowedLaws", "expression", "variables", "goal", "feedback"
    );
    private static final Set<String> LOGIC_STEP_FIELDS = Set.of("law", "lawId", "result", "from", "to", "note");
    private static final Set<String> LOGIC_LAW_IDS = Set.of(
            "DOUBLE_NEGATION", "DE_MORGAN", "DISTRIBUTIVE", "IDENTITY", "DOMINATION", "IDEMPOTENT",
            "COMPLEMENT", "ABSORPTION", "COMMUTATIVE", "ASSOCIATIVE", "IMPLICATION"
    );
    private static final Set<String> CONTROL_FIELDS = Set.of("min", "max", "step", "initial");
    private static final Set<String> SUCCESS_CONDITION_FIELDS = Set.of("kind", "target", "tolerance", "correctOptionId");
    private static final Set<String> FEEDBACK_FIELDS = Set.of("success", "failure");

    private final ObjectMapper objectMapper;
    private final LogicExpressionService logicExpressionService;

    public InteractiveConfigService(ObjectMapper objectMapper, LogicExpressionService logicExpressionService) {
        this.objectMapper = objectMapper;
        this.logicExpressionService = logicExpressionService;
    }

    public String validateAndNormalize(InteractionType interactionType, String interactionConfig) {
        InteractionType type = interactionType == null ? InteractionType.NONE : interactionType;
        if (type == InteractionType.NONE) {
            return null;
        }
        if (type == InteractionType.OTHER) {
            return optionalJson(interactionConfig);
        }
        if (!isTemplate(type)) {
            throw new ValidationException("Unsupported interactionType: " + type);
        }
        if (interactionConfig == null || interactionConfig.isBlank()) {
            throw new ValidationException("interactionConfig is required for " + type);
        }
        if (interactionConfig.trim().length() > MAX_CONFIG_LENGTH) {
            throw new ValidationException("interactionConfig must be " + MAX_CONFIG_LENGTH + " characters or fewer");
        }
        ObjectNode root = parseObject(interactionConfig);
        requireType(root, type);
        boolean practice = normalizeAndValidateMode(root, type);
        switch (type) {
            case GRAPH_2D -> validateGraph(root, practice);
            case FORMULA_EXPLORER -> validateFormula(root, practice);
            case VISUAL_LAYER -> validateVisualLayer(root, practice);
            case LOGIC_FLOW -> validateLogicFlow(root, practice);
            case QUIZ -> validateQuiz(root, practice);
            default -> throw new ValidationException("Unsupported interactionType: " + type);
        }
        return stringify(root);
    }

    public List<InteractiveTemplateDto> listTemplates() {
        Map<String, Object> graphVisualization = graphVisualizationDefault();
        Map<String, Object> graphPractice = graphPracticeDefault();
        Map<String, Object> formulaVisualization = formulaVisualizationDefault();
        Map<String, Object> formulaPractice = formulaPracticeDefault();
        Map<String, Object> visualLayer = visualLayerDefault();
        Map<String, Object> visualLayerPractice = visualLayerPracticeDefault();
        Map<String, Object> quizPractice = quizPracticeDefault();
        Map<String, Object> logicCircuitPractice = logicCircuitPracticeDefault();
        Map<String, Object> logicSimplifyPractice = logicSimplifyPracticeDefault();
        return List.of(
                new InteractiveTemplateDto(
                        InteractionType.GRAPH_2D,
                        "2D Graph",
                        "Plot a single-variable expression across a bounded x range.",
                        graphVisualization,
                        graphVisualization,
                        graphPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("expression", "Expression", "text", true, null, null, 160, null),
                                field("xMin", "X min", "number", true, -1000.0, 1000.0, null, null),
                                field("xMax", "X max", "number", true, -1000.0, 1000.0, null, null),
                                field("yMin", "Y min", "number", true, -1000.0, 1000.0, null, null),
                                field("yMax", "Y max", "number", true, -1000.0, 1000.0, null, null),
                                field("sampleCount", "Samples", "number", true, 20.0, 500.0, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.FORMULA_EXPLORER,
                        "Formula Explorer",
                        "Explore a formula by adjusting bounded variables.",
                        formulaVisualization,
                        formulaVisualization,
                        formulaPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("formula", "Formula", "text", true, null, null, 160, null),
                                field("variables", "Variables", "variable-list", true, null, null, null, null),
                                field("precision", "Precision", "number", true, 0.0, 6.0, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.VISUAL_LAYER,
                        "Visual Layer",
                        "Build a Canva-style hotspot layer with click-to-highlight behavior.",
                        visualLayerPractice,
                        visualLayer,
                        visualLayerPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("zones", "Zones", "visual-zones", true, null, null, null, null),
                                field("elements", "Elements", "visual-elements", true, null, null, null, null),
                                field("interactions", "Interactions", "visual-interactions", true, null, null, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.QUIZ,
                        "Quiz",
                        "Render a local multiple-choice check with immediate feedback.",
                        quizPractice,
                        quizPractice,
                        quizPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("question", "Question", "textarea", true, null, null, 500, null),
                                field("options", "Options", "quiz-options", true, null, null, null, null)
                        )
                ),
                new InteractiveTemplateDto(
                        InteractionType.LOGIC_FLOW,
                        "Logic Flow",
                        "Grade propositional logic circuits and equivalence simplification on the server.",
                        logicCircuitPractice,
                        logicCircuitPractice,
                        logicSimplifyPractice,
                        List.of(
                                field("title", "Title", "text", true, null, null, 120, null),
                                field("kind", "Kind", "select", true, null, null, null, List.of("CIRCUIT", "SIMPLIFY")),
                                field("expression", "Expression", "text", false, null, null, 1000, null),
                                field("start", "Starting expression", "text", false, null, null, 1000, null),
                                field("target", "Target expression", "text", false, null, null, 1000, null)
                        )
                )
        );
    }

    private Map<String, Object> graphVisualizationDefault() {
        return Map.of(
                "type", "GRAPH_2D",
                "mode", "VISUALIZATION",
                "title", "Function graph",
                "expression", "sin(x)",
                "xMin", -6.28,
                "xMax", 6.28,
                "yMin", -2,
                "yMax", 2,
                "sampleCount", 160
        );
    }

    private Map<String, Object> graphPracticeDefault() {
        return Map.ofEntries(
                Map.entry("type", "GRAPH_2D"),
                Map.entry("mode", "PRACTICE"),
                Map.entry("title", "Match the line"),
                Map.entry("prompt", "Adjust the slope until the graph passes through the target point."),
                Map.entry("expression", "m * x"),
                Map.entry("controls", Map.of("m", Map.of("min", 0, "max", 5, "step", 0.5, "initial", 1))),
                Map.entry("xMin", 0),
                Map.entry("xMax", 5),
                Map.entry("yMin", 0),
                Map.entry("yMax", 10),
                Map.entry("sampleCount", 120),
                Map.entry("successCondition", Map.of("kind", "POINT_ON_GRAPH", "target", Map.of("x", 2, "y", 6), "tolerance", 0.15)),
                Map.entry("feedback", Map.of("success", "Correct. The curve reaches the target point.", "failure", "Not yet. Adjust the controls and compare the curve to the target."))
        );
    }

    private Map<String, Object> formulaVisualizationDefault() {
        return Map.of(
                "type", "FORMULA_EXPLORER",
                "mode", "VISUALIZATION",
                "title", "Area explorer",
                "formula", "width * height",
                "variables", List.of(
                        Map.of("name", "width", "label", "Width", "min", 1, "max", 20, "step", 1, "initial", 6),
                        Map.of("name", "height", "label", "Height", "min", 1, "max", 20, "step", 1, "initial", 4)
                ),
                "precision", 2
        );
    }

    private Map<String, Object> formulaPracticeDefault() {
        return Map.of(
                "type", "FORMULA_EXPLORER",
                "mode", "PRACTICE",
                "title", "Target magnitude",
                "prompt", "Adjust x and y until the magnitude equals 5.",
                "formula", "sqrt(x^2 + y^2)",
                "variables", List.of(
                        Map.of("name", "x", "label", "X component", "min", 0, "max", 10, "step", 1, "initial", 0),
                        Map.of("name", "y", "label", "Y component", "min", 0, "max", 10, "step", 1, "initial", 0)
                ),
                "precision", 2,
                "successCondition", Map.of("kind", "EXPRESSION_EQUALS", "target", 5, "tolerance", 0.01),
                "feedback", Map.of("success", "Correct. The magnitude is 5.", "failure", "Not yet. Look for a Pythagorean triple.")
        );
    }

    private Map<String, Object> visualLayerDefault() {
        return Map.of(
                "type", "VISUAL_LAYER",
                "mode", "VISUALIZATION",
                "title", "Visual hotspot layer",
                "canvas", Map.of(
                        "width", 900,
                        "height", 520,
                        "backgroundText", "Draw zones and connect clicks to highlights."
                ),
                "zones", List.of(
                        Map.ofEntries(
                                Map.entry("id", "zone_a"),
                                Map.entry("label", "Zone A"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 270),
                                Map.entry("y", 150),
                                Map.entry("width", 220),
                                Map.entry("height", 220),
                                Map.entry("color", "#ffd333"),
                                Map.entry("highlightColor", "#ff8f1f"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "Zone A highlighted.")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_b"),
                                Map.entry("label", "Zone B"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 410),
                                Map.entry("y", 150),
                                Map.entry("width", 220),
                                Map.entry("height", 220),
                                Map.entry("color", "#8fb3ff"),
                                Map.entry("highlightColor", "#4f8cff"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "Zone B highlighted.")
                        )
                ),
                "elements", List.of(
                        Map.of("id", "choice_a", "label", "Highlight A", "kind", "button", "x", 40, "y", 40, "width", 150, "height", 48)
                ),
                "interactions", List.of(
                        Map.of("triggerId", "choice_a", "effect", "HIGHLIGHT_ZONE", "targetZoneId", "zone_a", "feedback", "Zone A highlighted.")
                )
        );
    }

    private Map<String, Object> visualLayerPracticeDefault() {
        return Map.ofEntries(
                Map.entry("type", "VISUAL_LAYER"),
                Map.entry("mode", "PRACTICE"),
                Map.entry("title", "Build the Venn diagram"),
                Map.entry("prompt", "Add the required circles, arrange the overlaps, then enter the value for each visible region."),
                Map.entry("canvas", Map.of("width", 900, "height", 520, "backgroundText", "")),
                Map.entry("zones", List.of(
                        Map.ofEntries(
                                Map.entry("id", "zone_a"),
                                Map.entry("label", "A"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 250),
                                Map.entry("y", 130),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 35),
                                Map.entry("labelY", 30),
                                Map.entry("color", "#ffd333"),
                                Map.entry("highlightColor", "#ff8f1f"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_b"),
                                Map.entry("label", "B"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 390),
                                Map.entry("y", 130),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 65),
                                Map.entry("labelY", 30),
                                Map.entry("color", "#8fb3ff"),
                                Map.entry("highlightColor", "#4f8cff"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        ),
                        Map.ofEntries(
                                Map.entry("id", "zone_c"),
                                Map.entry("label", "C"),
                                Map.entry("shape", "circle"),
                                Map.entry("x", 320),
                                Map.entry("y", 250),
                                Map.entry("width", 260),
                                Map.entry("height", 260),
                                Map.entry("labelX", 50),
                                Map.entry("labelY", 75),
                                Map.entry("color", "#8fe0aa"),
                                Map.entry("highlightColor", "#3aa66b"),
                                Map.entry("highlightOpacity", 0.82),
                                Map.entry("feedback", "")
                        )
                )),
                Map.entry("elements", List.of()),
                Map.entry("interactions", List.of()),
                Map.entry("overlap", Map.of(
                        "enabled", true,
                        "sourceZoneIds", List.of("zone_a", "zone_b", "zone_c"),
                        "inputs", List.of(
                                Map.of("id", "A_ONLY", "label", "A", "zoneIds", List.of("zone_a"), "value", 33, "kind", "total"),
                                Map.of("id", "B_ONLY", "label", "B", "zoneIds", List.of("zone_b"), "value", 26, "kind", "total"),
                                Map.of("id", "C_ONLY", "label", "C", "zoneIds", List.of("zone_c"), "value", 22, "kind", "total"),
                                Map.of("id", "A_AND_B", "label", "A ∩ B", "zoneIds", List.of("zone_a", "zone_b"), "value", 10, "kind", "intersection"),
                                Map.of("id", "A_AND_C", "label", "A ∩ C", "zoneIds", List.of("zone_a", "zone_c"), "value", 8, "kind", "intersection"),
                                Map.of("id", "B_AND_C", "label", "B ∩ C", "zoneIds", List.of("zone_b", "zone_c"), "value", 7, "kind", "intersection"),
                                Map.of("id", "A_AND_B_AND_C", "label", "A ∩ B ∩ C", "zoneIds", List.of("zone_a", "zone_b", "zone_c"), "value", 3, "kind", "intersection")
                        ),
                        "values", List.of()
                )),
                Map.entry("feedback", Map.of(
                        "success", "Correct. The regions match the expected values.",
                        "failure", "Not yet. Check that every required overlap exists and each region value is correct."
                ))
        );
    }

    private Map<String, Object> quizPracticeDefault() {
        return Map.of(
                "type", "QUIZ",
                "mode", "PRACTICE",
                "title", "Quick check",
                "prompt", "Choose the value that satisfies the equation.",
                "question", "Which value makes 2x + 5 = 13 true?",
                "options", List.of(
                        Map.of("id", "a", "label", "x = 3", "correct", false),
                        Map.of("id", "b", "label", "x = 4", "correct", true),
                        Map.of("id", "c", "label", "x = 6", "correct", false)
                ),
                "successCondition", Map.of("kind", "QUIZ_CORRECT_OPTION"),
                "feedback", Map.of("success", "Correct.", "failure", "Not quite. Try solving for x first.")
        );
    }

    private Map<String, Object> logicCircuitPracticeDefault() {
        return Map.of(
                "type", "LOGIC_FLOW",
                "kind", "CIRCUIT",
                "mode", "PRACTICE",
                "title", "Evaluate a logic statement",
                "expression", "P ∧ ¬Q",
                "goal", "MATCH_OUTPUT",
                "feedback", Map.of("success", "Correct.", "failure", "Not yet. Recheck each truth value.")
        );
    }

    private Map<String, Object> logicSimplifyPracticeDefault() {
        return Map.of(
                "type", "LOGIC_FLOW",
                "kind", "SIMPLIFY",
                "mode", "PRACTICE",
                "title", "Simplify an implication",
                "start", "P -> Q",
                "target", "¬P ∨ Q",
                "allowedLaws", List.of("IMPLICATION", "DOUBLE_NEGATION", "DE_MORGAN"),
                "feedback", Map.of("success", "Correct. The expression is logically equivalent.", "failure", "Not yet. Check the implication law.")
        );
    }

    private InteractiveFieldDto field(
            String path,
            String label,
            String inputType,
            boolean required,
            Double min,
            Double max,
            Integer maxLength,
            List<String> options
    ) {
        return new InteractiveFieldDto(path, label, inputType, required, min, max, maxLength, options);
    }

    private boolean isTemplate(InteractionType type) {
        return type == InteractionType.GRAPH_2D
                || type == InteractionType.FORMULA_EXPLORER
                || type == InteractionType.VISUAL_LAYER
                || type == InteractionType.LOGIC_FLOW
                || type == InteractionType.QUIZ;
    }

    private String optionalJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.trim().length() > MAX_CONFIG_LENGTH) {
            throw new ValidationException("interactionConfig must be " + MAX_CONFIG_LENGTH + " characters or fewer");
        }
        try {
            objectMapper.readTree(value);
            return value.trim();
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }

    private ObjectNode parseObject(String value) {
        try {
            JsonNode root = objectMapper.readTree(value);
            if (!root.isObject()) {
                throw new ValidationException("interactionConfig must be a JSON object");
            }
            return (ObjectNode) root;
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }

    private void requireType(ObjectNode root, InteractionType type) {
        String configType = requiredText(root, "type", 30);
        if (!type.name().equals(configType)) {
            throw new ValidationException("interactionConfig.type must match interactionType");
        }
    }

    private boolean normalizeAndValidateMode(ObjectNode root, InteractionType type) {
        JsonNode mode = root.get("mode");
        if (mode == null || mode.isNull()) {
            if (type == InteractionType.QUIZ) {
                normalizeQuizPracticeFields(root);
                return true;
            }
            root.put("mode", "VISUALIZATION");
            return false;
        }
        if (!mode.isTextual()) {
            throw new ValidationException("mode must be VISUALIZATION or PRACTICE");
        }
        String value = mode.asText().trim();
        if (!Set.of("VISUALIZATION", "PRACTICE").contains(value)) {
            throw new ValidationException("mode must be VISUALIZATION or PRACTICE");
        }
        if (type == InteractionType.QUIZ) {
            root.put("mode", "PRACTICE");
            normalizeQuizPracticeFields(root);
            return true;
        }
        root.put("mode", value);
        return "PRACTICE".equals(value);
    }

    private void normalizeQuizPracticeFields(ObjectNode root) {
        root.put("mode", "PRACTICE");
        if (!root.hasNonNull("prompt")) {
            String question = root.path("question").isTextual() ? root.path("question").asText() : "Choose the correct answer.";
            root.put("prompt", question);
        }
        JsonNode successCondition = root.get("successCondition");
        if (successCondition == null || successCondition.isNull()) {
            ObjectNode condition = root.putObject("successCondition");
            condition.put("kind", "QUIZ_CORRECT_OPTION");
        } else if (successCondition.isObject()) {
            ObjectNode condition = (ObjectNode) successCondition;
            JsonNode legacyType = condition.get("type");
            if (!condition.hasNonNull("kind") && legacyType != null && legacyType.isTextual()) {
                condition.put("kind", legacyType.asText());
            }
            condition.remove("type");
        }
        JsonNode feedback = root.get("feedback");
        if (feedback == null || feedback.isNull()) {
            ObjectNode feedbackObject = root.putObject("feedback");
            feedbackObject.put("success", "Correct.");
            feedbackObject.put("failure", "Not quite. Try again.");
        } else if (feedback.isObject()) {
            ObjectNode feedbackObject = (ObjectNode) feedback;
            if (!feedbackObject.hasNonNull("success")) {
                feedbackObject.put("success", "Correct.");
            }
            if (!feedbackObject.hasNonNull("failure")) {
                feedbackObject.put("failure", "Not quite. Try again.");
            }
        }
    }

    private void validateGraph(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, GRAPH_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        Set<String> variables = new java.util.HashSet<>(Set.of("x"));
        Map<String, double[]> controls = practice ? validateControls(root, null, 1, 6) : Map.of();
        variables.addAll(controls.keySet());
        String expression = requiredText(root, "expression", 160);
        validateExpression(expression, variables);
        double xMin = requiredNumber(root, "xMin", -1000, 1000);
        double xMax = requiredNumber(root, "xMax", -1000, 1000);
        double yMin = requiredNumber(root, "yMin", -1000, 1000);
        double yMax = requiredNumber(root, "yMax", -1000, 1000);
        if (xMax <= xMin) {
            throw new ValidationException("xMax must be greater than xMin");
        }
        if (yMax <= yMin) {
            throw new ValidationException("yMax must be greater than yMin");
        }
        requiredInteger(root, "sampleCount", 20, 500);
        if (practice) {
            validatePracticeCommon(root, "POINT_ON_GRAPH");
            validatePointOnGraphCondition((ObjectNode) root.get("successCondition"), xMin, xMax, yMin, yMax);
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateFormula(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, FORMULA_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        String formula = requiredText(root, "formula", 160);
        JsonNode variables = root.get("variables");
        if (variables == null || !variables.isArray() || variables.size() < 1 || variables.size() > 10) {
            throw new ValidationException("variables must contain 1 to 10 items");
        }
        Set<String> names = new java.util.HashSet<>();
        for (JsonNode variable : variables) {
            if (!variable.isObject()) {
                throw new ValidationException("variables must contain objects");
            }
            ObjectNode variableObject = (ObjectNode) variable;
            rejectUnknownFields(variableObject, VARIABLE_FIELDS, "variables");
            String name = requiredText(variableObject, "name", 30);
            if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,29}")) {
                throw new ValidationException("variable name must start with a letter and contain only letters, numbers, or underscores");
            }
            if (!names.add(name)) {
                throw new ValidationException("variable names must be unique");
            }
            requiredText(variableObject, "label", 80);
            double min = requiredNumber(variableObject, "min", -1000, 1000);
            double max = requiredNumber(variableObject, "max", -1000, 1000);
            if (max <= min) {
                throw new ValidationException("variable max must be greater than min");
            }
            double step = requiredNumber(variableObject, "step", 0.0001, 1000);
            double initial = requiredNumber(variableObject, "initial", min, max);
            if (step > max - min) {
                throw new ValidationException("variable step must fit inside the variable range");
            }
        }
        validateExpression(formula, names);
        validateFormulaOptions(root, names);
        requiredInteger(root, "precision", 0, 6);
        if (practice) {
            validatePracticeCommon(root, "EXPRESSION_EQUALS");
            validateExpressionEqualsCondition((ObjectNode) root.get("successCondition"));
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateFormulaOptions(ObjectNode root, Set<String> names) {
        JsonNode options = root.get("formulaOptions");
        if (options == null || options.isNull()) {
            return;
        }
        if (!options.isArray() || options.size() < 1 || options.size() > 10) {
            throw new ValidationException("formulaOptions must contain 1 to 10 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode option : options) {
            if (!option.isObject()) {
                throw new ValidationException("formulaOptions must contain objects");
            }
            ObjectNode optionObject = (ObjectNode) option;
            rejectUnknownFields(optionObject, FORMULA_OPTION_FIELDS, "formulaOptions");
            String id = requiredText(optionObject, "id", 40);
            if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,39}")) {
                throw new ValidationException("formula option id must start with a letter and contain only letters, numbers, underscores, or dashes");
            }
            if (!ids.add(id)) {
                throw new ValidationException("formula option ids must be unique");
            }
            requiredText(optionObject, "label", 120);
            validateExpression(requiredText(optionObject, "formula", 160), names);
            optionalText(optionObject, "description", 500);
            validateFormulaSteps(optionObject, names);
        }
    }

    private void validateFormulaSteps(ObjectNode optionObject, Set<String> names) {
        JsonNode steps = optionObject.get("steps");
        if (steps == null || steps.isNull()) {
            return;
        }
        if (!steps.isArray() || steps.size() < 1 || steps.size() > 8) {
            throw new ValidationException("formula steps must contain 1 to 8 items");
        }
        for (JsonNode step : steps) {
            if (!step.isObject()) {
                throw new ValidationException("formula steps must contain objects");
            }
            ObjectNode stepObject = (ObjectNode) step;
            rejectUnknownFields(stepObject, FORMULA_STEP_FIELDS, "formulaOptions.steps");
            requiredText(stepObject, "label", 120);
            validateExpression(requiredText(stepObject, "expression", 160), names);
            optionalText(stepObject, "explanation", 500);
        }
    }

    private void validateVisualLayer(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, VISUAL_LAYER_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        JsonNode canvas = root.get("canvas");
        if (canvas == null || !canvas.isObject()) {
            throw new ValidationException("canvas is required for VISUAL_LAYER");
        }
        ObjectNode canvasObject = (ObjectNode) canvas;
        rejectUnknownFields(canvasObject, VISUAL_CANVAS_FIELDS, "canvas");
        double canvasWidth = requiredNumber(canvasObject, "width", 320, 2400);
        double canvasHeight = requiredNumber(canvasObject, "height", 240, 1800);
        optionalText(canvasObject, "backgroundText", 500);

        Set<String> zoneIds = validateVisualZones(root, canvasWidth, canvasHeight);
        Set<String> elementIds = validateVisualElements(root, canvasWidth, canvasHeight);
        validateVisualInteractions(root, zoneIds, elementIds);
        validateVisualOverlap(root, zoneIds);
        if (practice) {
            validateVisualPracticeCommon(root);
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private Set<String> validateVisualZones(ObjectNode root, double canvasWidth, double canvasHeight) {
        JsonNode zones = root.get("zones");
        if (zones == null || !zones.isArray() || zones.size() > 40) {
            throw new ValidationException("zones must be an array with 0 to 40 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode zone : zones) {
            if (!zone.isObject()) {
                throw new ValidationException("zones must contain objects");
            }
            ObjectNode zoneObject = (ObjectNode) zone;
            rejectUnknownFields(zoneObject, VISUAL_ZONE_FIELDS, "zones");
            String id = requiredId(zoneObject, "id", "zone id");
            if (!ids.add(id)) {
                throw new ValidationException("zone ids must be unique");
            }
            requiredText(zoneObject, "label", 80);
            String shape = requiredText(zoneObject, "shape", 20);
            if (!Set.of("rectangle", "circle").contains(shape)) {
                throw new ValidationException("zone shape must be rectangle or circle");
            }
            validateVisualBounds(zoneObject, canvasWidth, canvasHeight);
            optionalNumber(zoneObject, "labelX", 0, 100);
            optionalNumber(zoneObject, "labelY", 0, 100);
            String color = requiredText(zoneObject, "color", 7);
            if (!color.matches("#[0-9A-Fa-f]{6}")) {
                throw new ValidationException("zone color must be a hex color");
            }
            String highlightColor = optionalText(zoneObject, "highlightColor", 7);
            if (highlightColor != null && !highlightColor.matches("#[0-9A-Fa-f]{6}")) {
                throw new ValidationException("zone highlightColor must be a hex color");
            }
            optionalNumber(zoneObject, "highlightOpacity", 0, 1);
            optionalText(zoneObject, "feedback", 500);
        }
        return ids;
    }

    private Set<String> validateVisualElements(ObjectNode root, double canvasWidth, double canvasHeight) {
        JsonNode elements = root.get("elements");
        if (elements == null || !elements.isArray() || elements.size() > 40) {
            throw new ValidationException("elements must be an array with 0 to 40 items");
        }
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode element : elements) {
            if (!element.isObject()) {
                throw new ValidationException("elements must contain objects");
            }
            ObjectNode elementObject = (ObjectNode) element;
            rejectUnknownFields(elementObject, VISUAL_ELEMENT_FIELDS, "elements");
            String id = requiredId(elementObject, "id", "element id");
            if (!ids.add(id)) {
                throw new ValidationException("element ids must be unique");
            }
            requiredText(elementObject, "label", 120);
            String kind = requiredText(elementObject, "kind", 20);
            if (!Set.of("button", "hotspot", "line").contains(kind)) {
                throw new ValidationException("element kind must be button, hotspot, or line");
            }
            if ("line".equals(kind)) {
                requiredNumber(elementObject, "x", 0, canvasWidth);
                requiredNumber(elementObject, "y", 0, canvasHeight);
                requiredNumber(elementObject, "width", 0, canvasWidth);
                requiredNumber(elementObject, "height", 0, canvasHeight);
                requiredNumber(elementObject, "x1", 0, canvasWidth);
                requiredNumber(elementObject, "y1", 0, canvasHeight);
                requiredNumber(elementObject, "x2", 0, canvasWidth);
                requiredNumber(elementObject, "y2", 0, canvasHeight);
                optionalNumber(elementObject, "qx", 0, canvasWidth);
                optionalNumber(elementObject, "qy", 0, canvasHeight);
                String flow = optionalText(elementObject, "flow", 20);
                if (flow != null && !Set.of("none", "forward", "backward").contains(flow)) {
                    throw new ValidationException("line flow must be none, forward, or backward");
                }
                String arrow = optionalText(elementObject, "arrow", 10);
                if (arrow != null && !Set.of("none", "start", "end", "both").contains(arrow)) {
                    throw new ValidationException("line arrow must be none, start, end, or both");
                }
                String color = optionalText(elementObject, "color", 7);
                if (color != null && !color.matches("#[0-9A-Fa-f]{6}")) {
                    throw new ValidationException("line color must be a hex color");
                }
                optionalNumber(elementObject, "strokeWidth", 1, 20);
            } else {
                validateVisualBounds(elementObject, canvasWidth, canvasHeight);
            }
        }
        return ids;
    }

    private void validateVisualInteractions(ObjectNode root, Set<String> zoneIds, Set<String> elementIds) {
        JsonNode interactions = root.get("interactions");
        if (interactions == null || !interactions.isArray() || interactions.size() > 80) {
            throw new ValidationException("interactions must be an array with 0 to 80 items");
        }
        for (JsonNode interaction : interactions) {
            if (!interaction.isObject()) {
                throw new ValidationException("interactions must contain objects");
            }
            ObjectNode interactionObject = (ObjectNode) interaction;
            rejectUnknownFields(interactionObject, VISUAL_INTERACTION_FIELDS, "interactions");
            String triggerId = requiredId(interactionObject, "triggerId", "triggerId");
            if (!elementIds.contains(triggerId) && !zoneIds.contains(triggerId)) {
                throw new ValidationException("interaction triggerId must reference a zone or element");
            }
            String effect = requiredText(interactionObject, "effect", 40);
            if (!Set.of("HIGHLIGHT_ZONE", "SHOW_FEEDBACK").contains(effect)) {
                throw new ValidationException("interaction effect is not supported");
            }
            if ("HIGHLIGHT_ZONE".equals(effect)) {
                String targetZoneId = requiredId(interactionObject, "targetZoneId", "targetZoneId");
                if (!zoneIds.contains(targetZoneId)) {
                    throw new ValidationException("targetZoneId must reference a zone");
                }
            }
            optionalText(interactionObject, "feedback", 500);
        }
    }

    private void validateVisualOverlap(ObjectNode root, Set<String> zoneIds) {
        JsonNode overlap = root.get("overlap");
        if (overlap == null || overlap.isNull()) {
            return;
        }
        if (!overlap.isObject()) {
            throw new ValidationException("overlap must be an object");
        }
        ObjectNode overlapObject = (ObjectNode) overlap;
        rejectUnknownFields(overlapObject, VISUAL_OVERLAP_FIELDS, "overlap");
        JsonNode enabled = overlapObject.get("enabled");
        if (enabled == null || !enabled.isBoolean()) {
            throw new ValidationException("overlap.enabled must be a boolean");
        }
        List<String> sourceZoneIds = requiredIdArray(overlapObject, "sourceZoneIds", "overlap.sourceZoneIds", 0, 5);
        if (!enabled.booleanValue()) {
            if (sourceZoneIds.isEmpty()) {
                root.remove("overlap");
                return;
            }
        } else {
            if (sourceZoneIds.isEmpty()) {
                throw new ValidationException("overlap.sourceZoneIds must contain 1 to 5 ids when enabled");
            }
        }
        Set<String> sourceSet = new LinkedHashSet<>(sourceZoneIds);
        if (sourceSet.size() != sourceZoneIds.size()) {
            throw new ValidationException("overlap.sourceZoneIds must be unique");
        }
        for (String sourceZoneId : sourceZoneIds) {
            if (!zoneIds.contains(sourceZoneId)) {
                throw new ValidationException("overlap.sourceZoneIds must reference existing zones");
            }
        }

        JsonNode inputs = overlapObject.get("inputs");
        if (inputs != null && !inputs.isNull()) {
            validateAndNormalizeOverlapInputs(overlapObject, inputs, sourceZoneIds, sourceSet);
            return;
        }

        JsonNode values = overlapObject.get("values");
        validateLegacyOverlapValues(values, sourceZoneIds, sourceSet);
    }

    private void validateAndNormalizeOverlapInputs(ObjectNode overlapObject, JsonNode inputs, List<String> sourceZoneIds, Set<String> sourceSet) {
        List<List<String>> combinations = overlapCombinations(sourceZoneIds);
        if (!inputs.isArray() || inputs.size() != combinations.size()) {
            throw new ValidationException("overlap.inputs must contain every source total and intersection");
        }
        Set<String> inputIds = new HashSet<>();
        Map<String, Double> inputValues = new java.util.HashMap<>();
        ArrayNode normalizedInputs = objectMapper.createArrayNode();
        for (JsonNode input : inputs) {
            if (!input.isObject()) {
                throw new ValidationException("overlap.inputs must contain objects");
            }
            ObjectNode inputObject = (ObjectNode) input;
            rejectUnknownFields(inputObject, VISUAL_OVERLAP_INPUT_FIELDS, "overlap.inputs");
            String id = requiredLongId(inputObject, "id", "overlap input id", 240);
            if (!inputIds.add(id)) {
                throw new ValidationException("overlap input ids must be unique");
            }
            requiredText(inputObject, "label", 120);
            List<String> inputZoneIds = requiredIdArray(inputObject, "zoneIds", "overlap.inputs.zoneIds", 1, sourceZoneIds.size());
            Set<String> inputSet = new LinkedHashSet<>(inputZoneIds);
            if (inputSet.size() != inputZoneIds.size()) {
                throw new ValidationException("overlap.inputs.zoneIds must be unique");
            }
            if (!sourceSet.containsAll(inputZoneIds)) {
                throw new ValidationException("overlap.inputs.zoneIds must be subsets of sourceZoneIds");
            }
            String expectedId = overlapRegionId(inputZoneIds);
            if (!expectedId.equals(id)) {
                throw new ValidationException("overlap input id must be deterministic for its zoneIds");
            }
            String kind = requiredText(inputObject, "kind", 20);
            String expectedKind = inputZoneIds.size() == 1 ? "total" : "intersection";
            if (!expectedKind.equals(kind)) {
                throw new ValidationException("overlap input kind must match its zoneIds");
            }
            double value = requiredNumber(inputObject, "value", -1_000_000_000, 1_000_000_000);
            inputValues.put(expectedId, value);
            ObjectNode normalized = objectMapper.createObjectNode();
            normalized.put("id", expectedId);
            normalized.put("label", inputObject.get("label").asText());
            normalized.set("zoneIds", stringArray(inputZoneIds));
            normalized.put("value", value);
            normalized.put("kind", expectedKind);
            normalizedInputs.add(normalized);
        }
        for (List<String> combination : combinations) {
            if (!inputValues.containsKey(overlapRegionId(combination))) {
                throw new ValidationException("overlap.inputs must contain every source total and intersection");
            }
        }

        Map<String, ObjectNode> existingValues = new java.util.HashMap<>();
        JsonNode currentValues = overlapObject.get("values");
        if (currentValues != null && currentValues.isArray()) {
            validateLegacyOverlapValues(currentValues, sourceZoneIds, sourceSet);
            for (JsonNode value : currentValues) {
                if (value.isObject() && value.has("id")) {
                    existingValues.put(value.get("id").asText(), (ObjectNode) value);
                }
            }
        } else if (currentValues != null && !currentValues.isNull()) {
            throw new ValidationException("overlap.values must be an array of generated regions");
        }

        ArrayNode normalizedValues = objectMapper.createArrayNode();
        for (List<String> regionZoneIds : combinations) {
            String id = overlapRegionId(regionZoneIds);
            double exact = 0;
            for (List<String> candidate : combinations) {
                if (candidate.containsAll(regionZoneIds)) {
                    double inclusive = inputValues.get(overlapRegionId(candidate));
                    exact += ((candidate.size() - regionZoneIds.size()) % 2 == 0 ? 1 : -1) * inclusive;
                }
            }
            if (exact < 0) {
                throw new ValidationException("overlap inputs produce negative exact region: " + id);
            }
            ObjectNode existing = existingValues.get(id);
            ObjectNode normalized = objectMapper.createObjectNode();
            normalized.put("id", id);
            normalized.put("label", existing != null && existing.has("label") && existing.get("label").isTextual()
                    ? existing.get("label").asText()
                    : overlapRegionLabel(regionZoneIds, sourceZoneIds.size()));
            normalized.set("zoneIds", stringArray(regionZoneIds));
            normalized.put("value", exact);
            if (existing != null && existing.has("feedback") && existing.get("feedback").isTextual()) {
                normalized.put("feedback", existing.get("feedback").asText());
            }
            normalizedValues.add(normalized);
        }
        overlapObject.set("inputs", normalizedInputs);
        overlapObject.set("values", normalizedValues);
    }

    private void validateLegacyOverlapValues(JsonNode values, List<String> sourceZoneIds, Set<String> sourceSet) {
        if (values == null || !values.isArray() || values.size() > ((1 << sourceZoneIds.size()) - 1)) {
            throw new ValidationException("overlap.values must be an array of generated regions");
        }
        Set<String> valueIds = new HashSet<>();
        for (JsonNode value : values) {
            if (!value.isObject()) {
                throw new ValidationException("overlap.values must contain objects");
            }
            ObjectNode valueObject = (ObjectNode) value;
            rejectUnknownFields(valueObject, VISUAL_OVERLAP_VALUE_FIELDS, "overlap.values");
            String id = requiredLongId(valueObject, "id", "overlap value id", 240);
            if (!valueIds.add(id)) {
                throw new ValidationException("overlap value ids must be unique");
            }
            requiredText(valueObject, "label", 120);
            List<String> regionZoneIds = requiredIdArray(valueObject, "zoneIds", "overlap.values.zoneIds", 1, sourceZoneIds.size());
            Set<String> regionSet = new LinkedHashSet<>(regionZoneIds);
            if (regionSet.size() != regionZoneIds.size()) {
                throw new ValidationException("overlap.values.zoneIds must be unique");
            }
            if (!sourceSet.containsAll(regionZoneIds)) {
                throw new ValidationException("overlap.values.zoneIds must be subsets of sourceZoneIds");
            }
            String expectedId = overlapRegionId(regionZoneIds);
            if (!expectedId.equals(id)) {
                throw new ValidationException("overlap value id must be deterministic for its zoneIds");
            }
            requiredNumber(valueObject, "value", -1_000_000_000, 1_000_000_000);
            optionalText(valueObject, "feedback", 500);
        }
    }

    private List<List<String>> overlapCombinations(List<String> sourceZoneIds) {
        List<String> sorted = new ArrayList<>(sourceZoneIds);
        Collections.sort(sorted);
        List<List<String>> combinations = new ArrayList<>();
        for (int mask = 1; mask < (1 << sorted.size()); mask++) {
            List<String> combination = new ArrayList<>();
            for (int index = 0; index < sorted.size(); index++) {
                if ((mask & (1 << index)) != 0) {
                    combination.add(sorted.get(index));
                }
            }
            combinations.add(combination);
        }
        return combinations;
    }

    private ArrayNode stringArray(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach(array::add);
        return array;
    }

    private String overlapRegionLabel(List<String> zoneIds, int sourceCount) {
        List<String> labels = zoneIds.stream().map(this::overlapIdToken).toList();
        String base = String.join(" ∩ ", labels);
        if (zoneIds.size() == 1) {
            return base + " only";
        }
        return zoneIds.size() < sourceCount ? base + " only" : base;
    }

    private void validateVisualBounds(ObjectNode node, double canvasWidth, double canvasHeight) {
        double x = requiredNumber(node, "x", 0, canvasWidth);
        double y = requiredNumber(node, "y", 0, canvasHeight);
        double width = requiredNumber(node, "width", 8, canvasWidth);
        double height = requiredNumber(node, "height", 8, canvasHeight);
        if (x + width > canvasWidth || y + height > canvasHeight) {
            throw new ValidationException("visual object must stay inside canvas bounds");
        }
    }

    private String requiredId(ObjectNode node, String fieldName, String label) {
        String id = requiredText(node, fieldName, 60);
        if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,59}")) {
            throw new ValidationException(label + " must start with a letter and contain only letters, numbers, underscores, or dashes");
        }
        return id;
    }

    private String requiredLongId(ObjectNode node, String fieldName, String label, int maxLength) {
        String id = requiredText(node, fieldName, maxLength);
        if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0," + (maxLength - 1) + "}")) {
            throw new ValidationException(label + " must start with a letter and contain only letters, numbers, underscores, or dashes");
        }
        return id;
    }

    private List<String> requiredIdArray(ObjectNode node, String fieldName, String label, int min, int max) {
        JsonNode values = node.get(fieldName);
        if (values == null || !values.isArray() || values.size() < min || values.size() > max) {
            throw new ValidationException(label + " must contain " + min + " to " + max + " ids");
        }
        List<String> ids = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual()) {
                throw new ValidationException(label + " must contain ids");
            }
            String id = value.asText().trim();
            if (!id.matches("[A-Za-z][A-Za-z0-9_-]{0,59}")) {
                throw new ValidationException(label + " entries must start with a letter and contain only letters, numbers, underscores, or dashes");
            }
            ids.add(id);
        }
        return ids;
    }

    private String overlapRegionId(List<String> zoneIds) {
        List<String> sorted = new ArrayList<>(zoneIds);
        Collections.sort(sorted);
        List<String> tokens = sorted.stream()
                .map(this::overlapIdToken)
                .toList();
        if (tokens.size() == 1) {
            return tokens.get(0) + "_ONLY";
        }
        return String.join("_AND_", tokens);
    }

    private String overlapIdToken(String zoneId) {
        String token = zoneId.replaceFirst("(?i)^zone[_-]?", "").replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        token = token.replaceAll("^_+|_+$", "");
        return token.isBlank() ? zoneId.replaceAll("[^A-Za-z0-9]+", "_").toUpperCase() : token;
    }

    private void validateQuiz(ObjectNode root, boolean practice) {
        root.remove(List.of("hint", "explanation"));
        rejectUnknownFields(root, QUIZ_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        requiredText(root, "question", 500);
        JsonNode options = root.get("options");
        if (options == null || !options.isArray() || options.size() < 2 || options.size() > 6) {
            throw new ValidationException("options must contain 2 to 6 items");
        }
        int correctCount = 0;
        Set<String> ids = new java.util.HashSet<>();
        for (JsonNode option : options) {
            if (!option.isObject()) {
                throw new ValidationException("options must contain objects");
            }
            ObjectNode optionObject = (ObjectNode) option;
            rejectUnknownFields(optionObject, QUIZ_OPTION_FIELDS, "options");
            String id = requiredText(optionObject, "id", 24);
            if (!ids.add(id)) {
                throw new ValidationException("option ids must be unique");
            }
            requiredText(optionObject, "label", 250);
            JsonNode correct = optionObject.get("correct");
            if (correct == null || !correct.isBoolean()) {
                throw new ValidationException("option correct must be true or false");
            }
            if (correct.booleanValue()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            throw new ValidationException("quiz must contain exactly one correct option");
        }
        if (practice) {
            validatePracticeCommon(root, "QUIZ_CORRECT_OPTION");
            JsonNode correctOptionId = root.get("successCondition").get("correctOptionId");
            if (correctOptionId != null && (!correctOptionId.isTextual() || !ids.contains(correctOptionId.asText()))) {
                throw new ValidationException("correctOptionId must match a quiz option id");
            }
        } else {
            rejectVisualizationPracticeFields(root);
        }
    }

    private void validateLogicFlow(ObjectNode root, boolean practice) {
        rejectUnknownFields(root, LOGIC_FIELDS, "interactionConfig");
        requiredText(root, "title", 120);
        optionalText(root, "prompt", 500);
        String kind = requiredText(root, "kind", 20);
        if (!Set.of("CIRCUIT", "SIMPLIFY").contains(kind)) {
            throw new ValidationException("kind must be CIRCUIT or SIMPLIFY");
        }
        if ("CIRCUIT".equals(kind)) {
            validateLogicCircuit(root, practice);
        } else {
            validateLogicSimplify(root, practice);
        }
    }

    private void validateLogicCircuit(ObjectNode root, boolean practice) {
        String expression = requiredText(root, "expression", 1000);
        LogicExpressionService.Node ast = logicExpressionService.parse(expression);
        if (logicExpressionService.variables(ast).size() > 8) {
            throw new ValidationException("logic expression can contain at most 8 variables");
        }
        JsonNode variables = root.get("variables");
        if (variables != null && !variables.isNull()) {
            if (!variables.isArray() || variables.size() > 8) {
                throw new ValidationException("variables must contain 0 to 8 items");
            }
            Set<String> expressionVariables = logicExpressionService.variables(ast);
            for (JsonNode variable : variables) {
                if (!variable.isTextual() || !expressionVariables.contains(variable.asText())) {
                    throw new ValidationException("variables must match expression variables");
                }
            }
        }
        String goal = optionalText(root, "goal", 40);
        if (goal != null && !Set.of("MATCH_OUTPUT", "TRUE", "FALSE", "EXPLORE").contains(goal)) {
            throw new ValidationException("goal must be MATCH_OUTPUT, TRUE, FALSE, or EXPLORE");
        }
        if (root.has("start") || root.has("target") || root.has("steps") || root.has("allowedLaws")) {
            throw new ValidationException("SIMPLIFY fields are not supported for CIRCUIT");
        }
        if (practice) {
            validateLogicFeedback(root);
        } else {
            rejectLogicPracticeFields(root);
        }
    }

    private void validateLogicSimplify(ObjectNode root, boolean practice) {
        String start = requiredText(root, "start", 1000);
        LogicExpressionService.Node startAst = logicExpressionService.parse(start);
        if (logicExpressionService.variables(startAst).size() > 8) {
            throw new ValidationException("logic expression can contain at most 8 variables");
        }
        String target = optionalText(root, "target", 1000);
        if (target != null) {
            LogicExpressionService.Node targetAst = logicExpressionService.parse(target);
            if (logicExpressionService.variables(targetAst).size() > 8) {
                throw new ValidationException("logic expression can contain at most 8 variables");
            }
        }
        validateLogicSteps(root);
        validateAllowedLogicLaws(root);
        if (root.has("expression") || root.has("variables") || root.has("goal")) {
            throw new ValidationException("CIRCUIT fields are not supported for SIMPLIFY");
        }
        if (practice) {
            validateLogicFeedback(root);
        } else {
            throw new ValidationException("SIMPLIFY supports PRACTICE mode only");
        }
    }

    private void validateLogicFeedback(ObjectNode root) {
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for LOGIC_FLOW practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);
    }

    private void rejectLogicPracticeFields(ObjectNode root) {
        root.remove("feedback");
    }

    private void validateLogicSteps(ObjectNode root) {
        JsonNode steps = root.get("steps");
        if (steps == null || steps.isNull()) {
            return;
        }
        if (!steps.isArray() || steps.size() > 20) {
            throw new ValidationException("steps must contain 0 to 20 items");
        }
        for (JsonNode step : steps) {
            if (!step.isObject()) {
                throw new ValidationException("steps must contain objects");
            }
            ObjectNode stepObject = (ObjectNode) step;
            rejectUnknownFields(stepObject, LOGIC_STEP_FIELDS, "steps");
            String law = optionalText(stepObject, "law", 80);
            String lawId = optionalText(stepObject, "lawId", 80);
            if (law != null && !LOGIC_LAW_IDS.contains(law)) {
                throw new ValidationException("steps law must be a known logic law id");
            }
            if (lawId != null && !LOGIC_LAW_IDS.contains(lawId)) {
                throw new ValidationException("steps lawId must be a known logic law id");
            }
            validateOptionalLogicExpression(stepObject, "result");
            validateOptionalLogicExpression(stepObject, "from");
            validateOptionalLogicExpression(stepObject, "to");
            optionalText(stepObject, "note", 500);
        }
    }

    private void validateAllowedLogicLaws(ObjectNode root) {
        JsonNode allowedLaws = root.get("allowedLaws");
        if (allowedLaws == null || allowedLaws.isNull()) {
            return;
        }
        if (!allowedLaws.isArray() || allowedLaws.size() > LOGIC_LAW_IDS.size()) {
            throw new ValidationException("allowedLaws must contain known logic law ids");
        }
        Set<String> ids = new HashSet<>();
        for (JsonNode law : allowedLaws) {
            if (!law.isTextual() || !LOGIC_LAW_IDS.contains(law.asText()) || !ids.add(law.asText())) {
                throw new ValidationException("allowedLaws must contain unique known logic law ids");
            }
        }
    }

    private void validateOptionalLogicExpression(ObjectNode node, String fieldName) {
        String expression = optionalText(node, fieldName, 1000);
        if (expression != null) {
            logicExpressionService.parse(expression);
        }
    }

    private void validatePracticeCommon(ObjectNode root, String expectedKind) {
        requiredText(root, "prompt", 500);
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);

        JsonNode condition = root.get("successCondition");
        if (condition == null || !condition.isObject()) {
            throw new ValidationException("successCondition is required for practice interactions");
        }
        ObjectNode conditionObject = (ObjectNode) condition;
        rejectUnknownFields(conditionObject, SUCCESS_CONDITION_FIELDS, "successCondition");
        String kind = requiredText(conditionObject, "kind", 40);
        if (!expectedKind.equals(kind)) {
            throw new ValidationException("successCondition.kind must be " + expectedKind);
        }
    }

    private void validateVisualPracticeCommon(ObjectNode root) {
        requiredText(root, "prompt", 500);
        JsonNode feedback = root.get("feedback");
        if (feedback == null || !feedback.isObject()) {
            throw new ValidationException("feedback is required for practice interactions");
        }
        ObjectNode feedbackObject = (ObjectNode) feedback;
        rejectUnknownFields(feedbackObject, FEEDBACK_FIELDS, "feedback");
        requiredText(feedbackObject, "success", 500);
        requiredText(feedbackObject, "failure", 500);
        JsonNode overlap = root.get("overlap");
        if (overlap == null || !overlap.isObject() || !overlap.path("enabled").asBoolean(false)) {
            throw new ValidationException("overlap is required for visual layer practice");
        }
        JsonNode sourceZoneIds = overlap.get("sourceZoneIds");
        if (sourceZoneIds == null || !sourceZoneIds.isArray() || sourceZoneIds.size() < 2 || sourceZoneIds.size() > 3) {
            throw new ValidationException("visual layer practice requires 2 to 3 overlap source zones");
        }
    }

    private void rejectVisualizationPracticeFields(ObjectNode root) {
        for (String field : PRACTICE_FIELDS) {
            root.remove(field);
        }
        root.remove("controls");
    }

    private Map<String, double[]> validateControls(ObjectNode root, Set<String> allowedNames, int minCount, int maxCount) {
        JsonNode controls = root.get("controls");
        if (controls == null || !controls.isObject()) {
            throw new ValidationException("controls is required for this practice interaction");
        }
        ObjectNode controlsObject = (ObjectNode) controls;
        int count = 0;
        Map<String, double[]> ranges = new java.util.HashMap<>();
        Iterator<String> names = controlsObject.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            count++;
            if (!name.matches("[A-Za-z][A-Za-z0-9_]{0,29}")) {
                throw new ValidationException("control name must start with a letter and contain only letters, numbers, or underscores");
            }
            if (allowedNames != null && !allowedNames.contains(name)) {
                throw new ValidationException("unsupported control: " + name);
            }
            JsonNode control = controlsObject.get(name);
            if (!control.isObject()) {
                throw new ValidationException("controls must contain objects");
            }
            ObjectNode controlObject = (ObjectNode) control;
            rejectUnknownFields(controlObject, CONTROL_FIELDS, "controls");
            double min = requiredNumber(controlObject, "min", -1000, 1000);
            double max = requiredNumber(controlObject, "max", -1000, 1000);
            if (max <= min) {
                throw new ValidationException("control max must be greater than min");
            }
            double step = requiredNumber(controlObject, "step", 0.0001, 1000);
            if (step > max - min) {
                throw new ValidationException("control step must fit inside the control range");
            }
            requiredNumber(controlObject, "initial", min, max);
            ranges.put(name, new double[] { min, max });
        }
        if (count < minCount || count > maxCount) {
            throw new ValidationException("controls must contain " + minCount + " to " + maxCount + " items");
        }
        return ranges;
    }

    private void validateExpressionEqualsCondition(ObjectNode condition) {
        requiredNumber(condition, "target", -1_000_000, 1_000_000);
        optionalNumber(condition, "tolerance", 0, 1_000_000);
    }

    private void validatePointOnGraphCondition(ObjectNode condition, double xMin, double xMax, double yMin, double yMax) {
        JsonNode target = condition.get("target");
        if (target == null || !target.isObject()) {
            throw new ValidationException("successCondition.target is required");
        }
        ObjectNode targetObject = (ObjectNode) target;
        rejectUnknownFields(targetObject, Set.of("x", "y"), "successCondition.target");
        requiredNumber(targetObject, "x", xMin, xMax);
        requiredNumber(targetObject, "y", yMin, yMax);
        optionalNumber(condition, "tolerance", 0, 1000);
    }

    private void validateExpression(String expression, Set<String> allowedVariables) {
        if (!expression.matches("[A-Za-z0-9_+\\-*/^().,\\s]+")) {
            throw new ValidationException("expression contains unsupported characters");
        }
        for (String token : expression.split("[^A-Za-z0-9_]+")) {
            if (token.isBlank() || token.matches("\\d+")) {
                continue;
            }
            if (allowedVariables.contains(token) || Set.of("sin", "cos", "tan", "sqrt", "abs", "log", "ln", "exp", "min", "max", "pow", "pi", "e").contains(token)) {
                continue;
            }
            throw new ValidationException("expression contains unsupported identifier: " + token);
        }
    }

    private void rejectUnknownFields(ObjectNode node, Set<String> allowedFields, String path) {
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowedFields.contains(field)) {
                throw new ValidationException(path + " contains unsupported field: " + field);
            }
        }
    }

    private String requiredText(ObjectNode node, String fieldName, int maxLength) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        node.put(fieldName, text);
        return text;
    }

    private String optionalText(ObjectNode node, String fieldName, int maxLength) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new ValidationException(fieldName + " must be text");
        }
        String text = value.asText().trim();
        if (text.length() > maxLength) {
            throw new ValidationException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        node.put(fieldName, text);
        return text;
    }

    private double requiredNumber(ObjectNode node, String fieldName, double min, double max) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isNumber()) {
            throw new ValidationException(fieldName + " is required");
        }
        double number = value.asDouble();
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    private Double optionalNumber(ObjectNode node, String fieldName, double min, double max) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isNumber()) {
            throw new ValidationException(fieldName + " must be a number");
        }
        double number = value.asDouble();
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    private int requiredInteger(ObjectNode node, String fieldName, int min, int max) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.canConvertToInt() || value.asDouble() % 1 != 0) {
            throw new ValidationException(fieldName + " must be an integer");
        }
        int number = value.asInt();
        if (number < min || number > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max);
        }
        return number;
    }

    private String stringify(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new ValidationException("interactionConfig must be valid JSON");
        }
    }
}
